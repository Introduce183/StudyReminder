package com.study.reminder.helper

import com.study.reminder.model.CourseSchedule
import java.text.SimpleDateFormat
import java.util.*

data class IcsParseResult(
    val courses: List<CourseSchedule>,
    val semesterStartDate: String,
    val warnings: List<String>
)

object IcsParser {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val icsDateOnlyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    fun parseIcs(content: String): IcsParseResult {
        val warnings = mutableListOf<String>()
        val allStartDates = mutableListOf<String>()

        val lines = unfoldLines(content)
        val events = splitIntoEvents(lines)

        val parsedEvents = mutableListOf<RawVEvent>()

        for (eventLines in events) {
            try {
                val parsed = parseVEvent(eventLines, warnings) ?: continue
                allStartDates.add(parsed.startDate)
                parsedEvents.add(parsed)
            } catch (e: Exception) {
                warnings.add("解析事件出错: ${e.message}")
            }
        }

        // Group by (courseName, dayOfWeek, startHour, startMinute, endHour, endMinute)
        val grouped = parsedEvents.groupBy { ev ->
            "${ev.summary}|${ev.dayOfWeek}|${ev.startHour}:${ev.startMinute}|${ev.endHour}:${ev.endMinute}"
        }

        val courses = mutableListOf<CourseSchedule>()

        for ((_, evts) in grouped) {
            val first = evts.first()

            // Collect week ranges: each VEVENT contributes [startDate, untilDate]
            val weekRanges = mutableListOf<List<String>>()
            val allExDates = mutableSetOf<String>()

            for (ev in evts) {
                allExDates.addAll(ev.exDates)
                if (ev.untilDate.isNotEmpty()) {
                    // UNTIL is the date of the last occurrence (Monday of last week)
                    // Extend by 6 days to cover the full final week (Mon-Sun)
                    weekRanges.add(listOf(ev.startDate, addDays(ev.untilDate, 6)))
                } else {
                    // No UNTIL = default to 20 weeks from start
                    val endDate = addWeeks(ev.startDate, 20)
                    weekRanges.add(listOf(ev.startDate, endDate))
                }
            }

            // Sort ranges by start date
            weekRanges.sortBy { it[0] }

            // Merge adjacent/overlapping ranges
            val merged = mergeRanges(weekRanges)

            courses.add(
                CourseSchedule(
                    courseName = first.summary,
                    location = first.location,
                    startHour = first.startHour,
                    startMinute = first.startMinute,
                    endHour = first.endHour,
                    endMinute = first.endMinute,
                    dayOfWeek = first.dayOfWeek,
                    weekRanges = merged,
                    exDates = allExDates
                )
            )
        }

        // Deduplicate: (courseName, dayOfWeek, startHour, startMinute, endHour, endMinute) same -> keep one
        val deduped = courses.distinctBy { "${it.courseName}_${it.dayOfWeek}_${it.startHour}_${it.startMinute}_${it.endHour}_${it.endMinute}" }

        // Semester start = earliest DTSTART
        val semesterStart = allStartDates.minOrNull() ?: ""

        return IcsParseResult(deduped, semesterStart, warnings)
    }

    // ── Line unfolding ─────────────────────────────────────────

    private fun unfoldLines(content: String): List<String> {
        val result = mutableListOf<String>()
        for (line in content.lines()) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    result[result.lastIndex] += line.substring(1)
                }
            } else {
                result.add(line)
            }
        }
        return result
    }

    // ── Split into VEVENT blocks ───────────────────────────────

    private fun splitIntoEvents(lines: List<String>): List<List<String>> {
        val events = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var inEvent = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed == "BEGIN:VEVENT" -> {
                    inEvent = true
                    current = mutableListOf()
                }
                trimmed == "END:VEVENT" -> {
                    inEvent = false
                    events.add(current)
                }
                inEvent -> current.add(trimmed)
            }
        }
        return events
    }

    // ── Parse single VEVENT ────────────────────────────────────

    private data class RawVEvent(
        val summary: String,
        val location: String,
        val startDate: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val dayOfWeek: Int,
        val untilDate: String,
        val exDates: Set<String>
    )

    private fun parseVEvent(lines: List<String>, warnings: MutableList<String>): RawVEvent? {
        var summary = ""
        var location = ""
        var dtStart = ""
        var dtEnd = ""
        var rrule = ""
        val exDates = mutableSetOf<String>()

        for (line in lines) {
            when {
                line.startsWith("SUMMARY:") -> summary = line.substringAfter("SUMMARY:")
                line.startsWith("LOCATION:") -> location = line.substringAfter("LOCATION:")
                line.startsWith("DTSTART") -> dtStart = line.substringAfter(":")
                line.startsWith("DTEND") -> dtEnd = line.substringAfter(":")
                line.startsWith("RRULE:") -> rrule = line.substringAfter("RRULE:")
                line.startsWith("EXDATE") -> {
                    val value = line.substringAfter(":")
                    for (part in value.split(",")) {
                        val dateStr = extractDate(part.trim())
                        if (dateStr.isNotEmpty()) exDates.add(dateStr)
                    }
                }
            }
        }

        if (summary.isEmpty()) return null
        if (dtStart.isEmpty()) return null

        val (startDate, startHour, startMinute) = parseDateTime(dtStart)
        val (_, endHour, endMinute) = if (dtEnd.isNotEmpty()) parseDateTime(dtEnd)
            else Triple(startDate, startHour + 1, startMinute)

        // Parse RRULE for UNTIL and BYDAY
        val rruleMap = parseRRule(rrule)
        val untilStr = rruleMap["UNTIL"] ?: ""
        val untilDate = extractDate(untilStr)
        val byDayStr = rruleMap["BYDAY"] ?: ""

        // Determine dayOfWeek: prefer RRULE BYDAY, fallback to DTSTART
        val dayOfWeek = if (byDayStr.isNotEmpty()) {
            parseBYDAY(byDayStr).firstOrNull() ?: dayOfWeekFromDateString(startDate)
        } else {
            dayOfWeekFromDateString(startDate)
        }

        // Check FREQ=WEEKLY
        val freq = rruleMap["FREQ"] ?: ""
        if (freq.isNotEmpty() && freq != "WEEKLY") {
            warnings.add("跳过非每周重复事件: $summary (FREQ=$freq)")
            return null
        }

        return RawVEvent(
            summary = summary,
            location = location,
            startDate = startDate,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            dayOfWeek = dayOfWeek,
            untilDate = untilDate,
            exDates = exDates
        )
    }

    // ── Date/time parsing ──────────────────────────────────────

    private fun parseDateTime(value: String): Triple<String, Int, Int> {
        // Supports: TZID=Asia/Shanghai:20250902T080000, 20250902T080000Z, 20250902T080000, 20250902
        val cleanValue = value.substringAfter(":")

        return try {
            when {
                cleanValue.contains("T") -> {
                    val dateStr = cleanValue.substringBefore("T")
                    val timeStr = cleanValue.substringAfter("T").removeSuffix("Z").take(6)
                    val date = icsDateOnlyFormat.parse(dateStr)
                    val hour = timeStr.substring(0, 2).toInt()
                    val minute = timeStr.substring(2, 4).toInt()
                    Triple(dateFormat.format(date!!), hour, minute)
                }
                cleanValue.length == 8 -> {
                    val date = icsDateOnlyFormat.parse(cleanValue)
                    Triple(dateFormat.format(date!!), 0, 0)
                }
                else -> Triple("", 0, 0)
            }
        } catch (_: Exception) { Triple("", 0, 0) }
    }

    private fun extractDate(value: String): String {
        if (value.isEmpty()) return ""
        val cleanValue = value.substringAfter(":")
        val dateStr = cleanValue.substringBefore("T").take(8)
        return try {
            val date = icsDateOnlyFormat.parse(dateStr)
            dateFormat.format(date!!)
        } catch (_: Exception) { "" }
    }

    private fun dayOfWeekFromDateString(dateStr: String): Int {
        return try {
            val date = dateFormat.parse(dateStr)!!
            val cal = Calendar.getInstance().apply { time = date }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7; else -> 0
            }
        } catch (_: Exception) { 0 }
    }

    // ── RRULE parsing ──────────────────────────────────────────

    private fun parseRRule(value: String): Map<String, String> {
        if (value.isEmpty()) return emptyMap()
        return value.split(";").associate {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
        }
    }

    private fun parseBYDAY(value: String): List<Int> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").mapNotNull { dayOfWeekFromIcs(it.trim()) }
    }

    private fun dayOfWeekFromIcs(code: String): Int? {
        val cleanCode = code.replace(Regex("^\\d+"), "")
        return when (cleanCode) {
            "MO" -> 1; "TU" -> 2; "WE" -> 3; "TH" -> 4
            "FR" -> 5; "SA" -> 6; "SU" -> 7; else -> null
        }
    }

    // ── Range merging ──────────────────────────────────────────

    private fun mergeRanges(ranges: List<List<String>>): List<List<String>> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it[0] }.toMutableList()
        val merged = mutableListOf(sorted[0])
        for (i in 1 until sorted.size) {
            val last = merged.last()
            val curr = sorted[i]
            // If ranges overlap or are adjacent (gap <= 1 day), merge
            if (curr[0] <= addDays(last[1], 1)) {
                val newEnd = if (curr[1] > last[1]) curr[1] else last[1]
                merged[merged.lastIndex] = listOf(last[0], newEnd)
            } else {
                merged.add(curr)
            }
        }
        return merged
    }

    private fun addWeeks(dateStr: String, weeks: Int): String = addDays(dateStr, weeks * 7)

    private fun addDays(dateStr: String, days: Int): String {
        return try {
            val date = dateFormat.parse(dateStr)!!
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            dateFormat.format(cal.time)
        } catch (_: Exception) { dateStr }
    }
}

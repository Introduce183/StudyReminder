package com.study.reminder.helper

import com.study.reminder.model.CourseSchedule
import com.study.reminder.model.WeekType
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
    private val icsDateTimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
    private val icsDateTimeZFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault())

    fun parseIcs(content: String): IcsParseResult {
        val warnings = mutableListOf<String>()
        val courses = mutableListOf<CourseSchedule>()
        val allStartDates = mutableListOf<String>()

        val lines = unfoldLines(content)
        val events = splitIntoEvents(lines)

        for (eventLines in events) {
            try {
                val parsed = parseVEvent(eventLines, warnings) ?: continue
                allStartDates.add(parsed.startDate)

                // 只处理 FREQ=WEEKLY 的事件
                if (parsed.freq != "WEEKLY") {
                    warnings.add("跳过非每周重复事件: ${parsed.summary}")
                    continue
                }

                val byDays = parsed.byDays
                if (byDays.isEmpty()) {
                    warnings.add("跳过无星期信息的事件: ${parsed.summary}")
                    continue
                }

                // 计算 endDate
                val endDate = calculateEndDate(parsed)

                // 计算 weekType 和 biweekly
                val isBiweekly = parsed.interval >= 2
                val weekType = determineWeekType(parsed)

                // 为每个 BYDAY 生成一个 CourseSchedule
                for (dayOfWeek in byDays) {
                    courses.add(
                        CourseSchedule(
                            courseName = parsed.summary,
                            location = parsed.location,
                            startHour = parsed.startHour,
                            startMinute = parsed.startMinute,
                            endHour = parsed.endHour,
                            endMinute = parsed.endMinute,
                            dayOfWeek = dayOfWeek,
                            weekType = weekType,
                            courseStartDate = parsed.startDate,
                            biweekly = isBiweekly,
                            endDate = endDate,
                            exDates = parsed.exDates
                        )
                    )
                }
            } catch (e: Exception) {
                warnings.add("解析事件出错: ${e.message}")
            }
        }

        // 去重: (courseName, dayOfWeek, startHour, startMinute) 相同的只保留一个
        val deduped = courses.distinctBy { "${it.courseName}_${it.dayOfWeek}_${it.startHour}_${it.startMinute}" }

        // 学期起始日 = 最早的 DTSTART 日期
        val semesterStart = allStartDates.minOrNull() ?: ""

        return IcsParseResult(deduped, semesterStart, warnings)
    }

    // ── 行展开 ─────────────────────────────────────────────

    private fun unfoldLines(content: String): List<String> {
        val result = mutableListOf<String>()
        for (line in content.lines()) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                // 折叠行：拼接到上一行
                if (result.isNotEmpty()) {
                    result[result.lastIndex] += line.substring(1)
                }
            } else {
                result.add(line)
            }
        }
        return result
    }

    // ── 分割 VEVENT 块 ─────────────────────────────────────

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

    // ── 解析单个 VEVENT ────────────────────────────────────

    private data class ParsedEvent(
        val summary: String,
        val location: String,
        val startDate: String,  // yyyy-MM-dd
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val freq: String,
        val interval: Int,
        val byDays: List<Int>,
        val count: Int,
        val until: String,
        val exDates: Set<String>
    )

    private fun parseVEvent(lines: List<String>, warnings: MutableList<String>): ParsedEvent? {
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
                    // EXDATE 可能有多个值，逗号分隔
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
        val (_, endHour, endMinute) = if (dtEnd.isNotEmpty()) parseDateTime(dtEnd) else Triple(startDate, startHour + 1, startMinute)

        // 解析 RRULE
        val rruleMap = parseRRule(rrule)
        val freq = rruleMap["FREQ"] ?: ""
        val interval = rruleMap["INTERVAL"]?.toIntOrNull() ?: 1
        val byDays = parseBYDAY(rruleMap["BYDAY"] ?: "")
        val count = rruleMap["COUNT"]?.toIntOrNull() ?: 0
        val until = extractDate(rruleMap["UNTIL"] ?: "")

        return ParsedEvent(
            summary = summary,
            location = location,
            startDate = startDate,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            freq = freq,
            interval = interval,
            byDays = byDays,
            count = count,
            until = until,
            exDates = exDates
        )
    }

    // ── 日期时间解析 ───────────────────────────────────────

    private fun parseDateTime(value: String): Triple<String, Int, Int> {
        // 支持格式:
        // 20240902T080000Z
        // 20240902T080000
        // TZID=Asia/Shanghai:20240902T080000
        // VALUE=DATE:20240902
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

    // ── RRULE 解析 ─────────────────────────────────────────

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
        // BYDAY 可能带数字前缀，如 "1MO"（第一个周一）
        val cleanCode = code.replace(Regex("^\\d+"), "")
        return when (cleanCode) {
            "MO" -> 1; "TU" -> 2; "WE" -> 3; "TH" -> 4
            "FR" -> 5; "SA" -> 6; "SU" -> 7; else -> null
        }
    }

    // ── 计算 endDate ───────────────────────────────────────

    private fun calculateEndDate(event: ParsedEvent): String {
        return when {
            event.until.isNotEmpty() -> event.until
            event.count > 0 -> {
                try {
                    val start = dateFormat.parse(event.startDate)!!
                    val cal = Calendar.getInstance().apply { time = start }
                    val intervalDays = if (event.interval > 1) event.interval * 7 else 7
                    cal.add(Calendar.DAY_OF_YEAR, (event.count - 1) * intervalDays)
                    dateFormat.format(cal.time)
                } catch (_: Exception) { "" }
            }
            else -> "" // 无限
        }
    }

    // ── 判断单双周 ─────────────────────────────────────────

    private fun determineWeekType(event: ParsedEvent): WeekType {
        if (event.interval < 2) return WeekType.ALL
        // INTERVAL=2 表示隔周，暂时返回 ALL
        // 实际单双周由 CourseScheduleRepository.resolveWeekType() 在用户设置学期起始日后计算
        return WeekType.ALL
    }
}

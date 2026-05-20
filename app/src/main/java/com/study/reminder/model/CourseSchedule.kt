package com.study.reminder.model

import org.json.JSONArray
import org.json.JSONObject

data class CourseSchedule(
    val id: Long = System.currentTimeMillis(),
    val courseName: String,
    val location: String = "",
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val dayOfWeek: Int,              // 1=周一..7=周日
    // weekRanges: list of [startDate, endDate] pairs (yyyy-MM-dd)
    // e.g. [["2025-09-01","2025-10-06"],["2025-10-20","2025-11-24"]]
    // represents weeks 1-5 and 7-9 active
    val weekRanges: List<List<String>> = emptyList(),
    val exDates: Set<String> = emptySet(),
    val enabled: Boolean = true
) {
    val startLabel: String get() = String.format("%02d:%02d", startHour, startMinute)
    val endLabel: String get() = String.format("%02d:%02d", endHour, endMinute)
    val dayLabel: String get() = when (dayOfWeek) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
        5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
    }

    /** Detect week pattern from ranges for display */
    val weekTypeLabel: String get() {
        if (weekRanges.size <= 1) return ""
        // Check if ranges are alternating (biweekly)
        val sorted = weekRanges.sortedBy { it[0] }
        for (i in 0 until sorted.size - 1) {
            val gapDays = daysBetween(sorted[i][1], sorted[i + 1][0])
            if (gapDays > 7) return "间隔周"
        }
        return ""
    }

    /** Check if the course is active on a given date */
    fun isActiveOnDate(date: String): Boolean {
        if (date in exDates) return false
        for (range in weekRanges) {
            if (date >= range[0] && date <= range[1]) return true
        }
        return false
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("courseName", courseName)
        put("location", location)
        put("startHour", startHour)
        put("startMinute", startMinute)
        put("endHour", endHour)
        put("endMinute", endMinute)
        put("dayOfWeek", dayOfWeek)
        val rangesArr = JSONArray()
        for (range in weekRanges) {
            val rangeArr = JSONArray()
            rangeArr.put(range[0])
            rangeArr.put(range[1])
            rangesArr.put(rangeArr)
        }
        put("weekRanges", rangesArr)
        put("exDates", JSONArray(exDates.toList()))
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(json: JSONObject) = CourseSchedule(
            id = json.getLong("id"),
            courseName = json.getString("courseName"),
            location = json.optString("location", ""),
            startHour = json.getInt("startHour"),
            startMinute = json.getInt("startMinute"),
            endHour = json.getInt("endHour"),
            endMinute = json.getInt("endMinute"),
            dayOfWeek = json.getInt("dayOfWeek"),
            weekRanges = parseWeekRanges(json),
            exDates = try {
                val arr = json.optJSONArray("exDates")
                if (arr != null) (0 until arr.length()).map { arr.getString(it) }.toSet()
                else emptySet()
            } catch (_: Exception) { emptySet() },
            enabled = json.optBoolean("enabled", true)
        )

        private fun parseWeekRanges(json: JSONObject): List<List<String>> {
            val arr = json.optJSONArray("weekRanges") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val rangeArr = arr.optJSONArray(i) ?: return@mapNotNull null
                if (rangeArr.length() >= 2) {
                    listOf(rangeArr.getString(0), rangeArr.getString(1))
                } else null
            }
        }

        private fun daysBetween(d1: String, d2: String): Int {
            return try {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val date1 = fmt.parse(d1)!!
                val date2 = fmt.parse(d2)!!
                ((date2.time - date1.time) / (24 * 60 * 60 * 1000)).toInt()
            } catch (_: Exception) { 0 }
        }
    }
}

package com.study.reminder.model

import org.json.JSONArray
import org.json.JSONObject

enum class WeekType {
    ALL,   // 每周
    ODD,   // 单周
    EVEN   // 双周
}

data class CourseSchedule(
    val id: Long = System.currentTimeMillis(),
    val courseName: String,
    val location: String = "",
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val dayOfWeek: Int,              // 1=周一..7=周日
    val weekType: WeekType = WeekType.ALL,
    val courseStartDate: String = "", // 课程原始 DTSTART 日期，用于单双周计算
    val biweekly: Boolean = false,   // 是否隔周（INTERVAL=2）
    val endDate: String = "",        // yyyy-MM-dd，空=无限
    val exDates: Set<String> = emptySet(), // 排除日期
    val enabled: Boolean = true
) {
    val startLabel: String get() = String.format("%02d:%02d", startHour, startMinute)
    val endLabel: String get() = String.format("%02d:%02d", endHour, endMinute)
    val dayLabel: String get() = when (dayOfWeek) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
        5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
    }
    val weekTypeLabel: String get() = when (weekType) {
        WeekType.ALL -> ""
        WeekType.ODD -> "单周"
        WeekType.EVEN -> "双周"
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
        put("weekType", weekType.name)
        put("courseStartDate", courseStartDate)
        put("biweekly", biweekly)
        put("endDate", endDate)
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
            weekType = try { WeekType.valueOf(json.optString("weekType", "ALL")) } catch (_: Exception) { WeekType.ALL },
            courseStartDate = json.optString("courseStartDate", ""),
            biweekly = json.optBoolean("biweekly", false),
            endDate = json.optString("endDate", ""),
            exDates = try {
                val arr = json.optJSONArray("exDates")
                if (arr != null) (0 until arr.length()).map { arr.getString(it) }.toSet()
                else emptySet()
            } catch (_: Exception) { emptySet() },
            enabled = json.optBoolean("enabled", true)
        )
    }
}

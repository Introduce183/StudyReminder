package com.study.reminder.model

import org.json.JSONObject

/**
 * 固定学习时间段
 * 例如：周一到周五 10:00 ~ 11:30
 */
data class StudySlot(
    val id: Long = System.currentTimeMillis(),
    val label: String = "",          // 备注，如"上午自习"
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val enableDays: Set<Int> = setOf(1,2,3,4,5,6,7), // 1=周一 7=周日，默认每天
    val enabled: Boolean = true
) {
    val startLabel: String get() = String.format("%02d:%02d", startHour, startMinute)
    val endLabel: String get() = String.format("%02d:%02d", endHour, endMinute)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("startHour", startHour)
        put("startMinute", startMinute)
        put("endHour", endHour)
        put("endMinute", endMinute)
        put("enableDays", enableDays.joinToString(","))
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(json: JSONObject) = StudySlot(
            id = json.getLong("id"),
            label = json.optString("label", ""),
            startHour = json.getInt("startHour"),
            startMinute = json.getInt("startMinute"),
            endHour = json.getInt("endHour"),
            endMinute = json.getInt("endMinute"),
            enableDays = json.optString("enableDays", "1,2,3,4,5,6,7")
                .split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
            enabled = json.optBoolean("enabled", true)
        )
    }
}

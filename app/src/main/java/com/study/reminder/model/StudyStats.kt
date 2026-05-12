package com.study.reminder.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class StudySession(
    val date: String,        // yyyy-MM-dd
    val startMs: Long,       // 开始时间戳
    val endMs: Long,         // 结束时间戳
    val durationMin: Int     // 实际学习分钟数
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("startMs", startMs)
        put("endMs", endMs)
        put("durationMin", durationMin)
    }

    companion object {
        fun fromJson(json: JSONObject) = StudySession(
            date = json.getString("date"),
            startMs = json.getLong("startMs"),
            endMs = json.getLong("endMs"),
            durationMin = json.getInt("durationMin")
        )
    }
}

data class DistractionEvent(
    val date: String,     // yyyy-MM-dd
    val timestamp: Long   // 时间戳
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject) = DistractionEvent(
            date = json.getString("date"),
            timestamp = json.getLong("timestamp")
        )
    }
}

object StudyStatsRepository {

    private const val PREF_SESSIONS = "study_sessions"
    private const val PREF_DISTRACTIONS = "distraction_events"
    private const val PREF_ACTIVE_START = "active_session_start"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── 会话管理 ──────────────────────────────────────────────

    fun startSession(context: Context, slotId: Long) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        if (prefs.getLong(PREF_ACTIVE_START, 0L) > 0) {
            endSession(context)
        }
        prefs.edit()
            .putLong(PREF_ACTIVE_START, System.currentTimeMillis())
            .apply()
    }

    fun endSession(context: Context) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val startMs = prefs.getLong(PREF_ACTIVE_START, 0L)
        if (startMs <= 0L) return

        val endMs = System.currentTimeMillis()
        val durationMin = ((endMs - startMs) / 60_000).toInt().coerceAtLeast(0)

        // 清除活跃会话标记
        prefs.edit().remove(PREF_ACTIVE_START).apply()

        if (durationMin < 1) return

        val date = dateFormat.format(Date(startMs))
        val session = StudySession(date, startMs, endMs, durationMin)

        val sessions = getSessions(context).toMutableList()
        sessions.add(session)
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -90) }.timeInMillis
        saveSessions(context, sessions.filter { it.endMs > cutoff })
    }

    fun hasActiveSession(context: Context): Boolean {
        return context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .getLong(PREF_ACTIVE_START, 0L) > 0
    }

    /**
     * 检查当前是否在某个学习时间段内，返回剩余分钟数（不在任何时间段内返回 0）
     */
    fun getActiveSlotRemainingMinutes(context: Context): Int {
        val slots = SlotRepository.getSlots(context)
        val cal = Calendar.getInstance()
        // Calendar: 1=周日, 2=周一 ... 7=周六 → StudySlot: 1=周一 ... 7=周日
        val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 0
        }
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        var maxRemaining = 0
        for (slot in slots) {
            if (!slot.enabled) continue
            if (dow !in slot.enableDays) continue
            val startMin = slot.startHour * 60 + slot.startMinute
            val endMin = slot.endHour * 60 + slot.endMinute
            if (nowMin in startMin until endMin) {
                val remaining = endMin - nowMin
                if (remaining > maxRemaining) maxRemaining = remaining
            }
        }
        return maxRemaining
    }

    // ── 摸鱼记录（独立于会话，立即保存）─────────────────────────

    fun recordDistraction(context: Context) {
        val now = System.currentTimeMillis()
        val date = dateFormat.format(Date(now))
        val event = DistractionEvent(date, now)

        val events = getDistractions(context).toMutableList()
        events.add(event)
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -90) }.timeInMillis
        saveDistractions(context, events.filter { it.timestamp > cutoff })
    }

    // ── 数据读写 ──────────────────────────────────────────────

    private fun getSessions(context: Context): List<StudySession> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_SESSIONS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { StudySession.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveSessions(context: Context, sessions: List<StudySession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_SESSIONS, arr.toString()).apply()
    }

    private fun getDistractions(context: Context): List<DistractionEvent> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_DISTRACTIONS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { DistractionEvent.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveDistractions(context: Context, events: List<DistractionEvent>) {
        val arr = JSONArray()
        events.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_DISTRACTIONS, arr.toString()).apply()
    }

    // ── 统计查询 ──────────────────────────────────────────────

    fun getTodayStats(context: Context): DayStats {
        val today = dateFormat.format(Date())
        val sessions = getSessions(context).filter { it.date == today }
        val distractions = getDistractions(context).count { it.date == today }
        return DayStats(today, "今日", sessions.sumOf { it.durationMin }, distractions, sessions.size)
    }

    fun getWeekStats(context: Context): List<DayStats> {
        val cal = Calendar.getInstance()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val sessions = getSessions(context)
        val distractions = getDistractions(context)
        val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val result = mutableListOf<DayStats>()

        for (i in 0 until 7) {
            val date = dateFormat.format(cal.time)
            val daySessions = sessions.filter { it.date == date }
            val dayDistractions = distractions.count { it.date == date }
            result.add(DayStats(date, dayNames[i], daySessions.sumOf { it.durationMin }, dayDistractions, daySessions.size))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun getMonthStats(context: Context): List<DayStats> {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.set(year, month, 1)

        val sessions = getSessions(context)
        val distractions = getDistractions(context)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = mutableListOf<DayStats>()

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val date = dateFormat.format(cal.time)
            val daySessions = sessions.filter { it.date == date }
            val dayDistractions = distractions.count { it.date == date }
            result.add(DayStats(date, "$day", daySessions.sumOf { it.durationMin }, dayDistractions, daySessions.size))
        }
        return result
    }
}

data class DayStats(
    val date: String,
    val label: String,
    val totalMinutes: Int,
    val distractionCount: Int,
    val sessionCount: Int
)

package com.study.reminder.model

import android.content.Context
import android.util.Log
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
    private const val PREF_ACTIVE_SLOT_ID = "active_slot_id"
    private val dateFormatTL = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    private val dateFormat: SimpleDateFormat get() = dateFormatTL.get()!!

    // In-memory cache to avoid repeated JSON parsing
    @Volatile private var cachedSessions: List<StudySession>? = null
    @Volatile private var cachedDistractions: List<DistractionEvent>? = null

    // ── 会话管理 ──────────────────────────────────────────────

    fun startSession(context: Context, slotId: Long) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        if (prefs.getLong(PREF_ACTIVE_START, 0L) > 0) {
            endSession(context)
        }
        prefs.edit()
            .putLong(PREF_ACTIVE_START, System.currentTimeMillis())
            .putLong(PREF_ACTIVE_SLOT_ID, slotId)
            .apply()
    }

    fun endSession(context: Context) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val startMs = prefs.getLong(PREF_ACTIVE_START, 0L)
        if (startMs <= 0L) return

        val endMs = System.currentTimeMillis()
        val durationMin = ((endMs - startMs) / 60_000).toInt().coerceAtLeast(0)

        // 清除活跃会话标记
        prefs.edit().remove(PREF_ACTIVE_START).remove(PREF_ACTIVE_SLOT_ID).apply()

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
     * 获取当前活跃会话的已持续分钟数，没有活跃会话返回 0
     */
    fun getActiveSessionMinutes(context: Context): Int {
        val startMs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .getLong(PREF_ACTIVE_START, 0L)
        if (startMs <= 0L) return 0
        return ((System.currentTimeMillis() - startMs) / 60_000).toInt().coerceAtLeast(0)
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

        // 检查课表
        val activeCourses = CourseScheduleRepository.getActiveCoursesNow(context)
        for (course in activeCourses) {
            val endMin = course.endHour * 60 + course.endMinute
            val remaining = endMin - nowMin
            if (remaining > maxRemaining) maxRemaining = remaining
        }

        return maxRemaining
    }

    /**
     * 获取当前活跃的时间段 ID，没有活跃时间段返回 -1L
     */
    fun getActiveSlotId(context: Context): Long {
        val slots = SlotRepository.getSlots(context)
        val cal = Calendar.getInstance()
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

        for (slot in slots) {
            if (!slot.enabled) continue
            if (dow !in slot.enableDays) continue
            val startMin = slot.startHour * 60 + slot.startMinute
            val endMin = slot.endHour * 60 + slot.endMinute
            if (nowMin in startMin until endMin) {
                return slot.id
            }
        }
        return -1L
    }

    /**
     * 获取所有当前活跃的时间段 ID 列表
     */
    fun getActiveSlotIds(context: Context): List<Long> {
        val slots = SlotRepository.getSlots(context)
        val cal = Calendar.getInstance()
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

        return slots.filter { slot ->
            slot.enabled && dow in slot.enableDays &&
            nowMin in (slot.startHour * 60 + slot.startMinute) until (slot.endHour * 60 + slot.endMinute)
        }.map { it.id }
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
        cachedSessions?.let { return it }
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_SESSIONS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { StudySession.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            Log.w("StudyStats", "Failed to parse sessions JSON, resetting", e)
            emptyList()
        }.also { cachedSessions = it }
    }

    private fun saveSessions(context: Context, sessions: List<StudySession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_SESSIONS, arr.toString()).apply()
        cachedSessions = sessions
    }

    private fun getDistractions(context: Context): List<DistractionEvent> {
        cachedDistractions?.let { return it }
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_DISTRACTIONS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { DistractionEvent.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            Log.w("StudyStats", "Failed to parse distractions JSON, resetting", e)
            emptyList()
        }.also { cachedDistractions = it }
    }

    private fun saveDistractions(context: Context, events: List<DistractionEvent>) {
        val arr = JSONArray()
        events.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_DISTRACTIONS, arr.toString()).apply()
        cachedDistractions = events
    }

    // ── 统计查询 ──────────────────────────────────────────────

    fun getTodayStats(context: Context): DayStats {
        val today = dateFormat.format(Date())
        val sessions = getSessions(context).filter { it.date == today }
        val distractions = getDistractions(context).count { it.date == today }
        val activeMinutes = getActiveSessionMinutes(context)
        return DayStats(today, "今日", sessions.sumOf { it.durationMin } + activeMinutes, distractions, sessions.size)
    }

    fun getWeekStats(context: Context, weekOffset: Int = 0): List<DayStats> {
        val cal = Calendar.getInstance()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
        val sessions = getSessions(context)
        val distractions = getDistractions(context)
        val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val result = mutableListOf<DayStats>()

        val today = dateFormat.format(Date())
        val activeMinutes = getActiveSessionMinutes(context)

        for (i in 0 until 7) {
            val date = dateFormat.format(cal.time)
            val daySessions = sessions.filter { it.date == date }
            val dayDistractions = distractions.count { it.date == date }
            val extraMinutes = if (date == today) activeMinutes else 0
            result.add(DayStats(date, dayNames[i], daySessions.sumOf { it.durationMin } + extraMinutes, dayDistractions, daySessions.size))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun getWeekDateRange(weekOffset: Int = 0): Pair<String, String> {
        val cal = Calendar.getInstance()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
        val start = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val end = dateFormat.format(cal.time)
        return start to end
    }

    fun getMonthStats(context: Context, monthOffset: Int = 0): List<DayStats> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.set(year, month, 1)

        val sessions = getSessions(context)
        val distractions = getDistractions(context)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = mutableListOf<DayStats>()

        val today = dateFormat.format(Date())
        val activeMinutes = getActiveSessionMinutes(context)

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val date = dateFormat.format(cal.time)
            val daySessions = sessions.filter { it.date == date }
            val dayDistractions = distractions.count { it.date == date }
            val extraMinutes = if (date == today) activeMinutes else 0
            result.add(DayStats(date, "$day", daySessions.sumOf { it.durationMin } + extraMinutes, dayDistractions, daySessions.size))
        }
        return result
    }

    fun getMonthLabel(monthOffset: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        return "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }
}

data class DayStats(
    val date: String,
    val label: String,
    val totalMinutes: Int,
    val distractionCount: Int,
    val sessionCount: Int
)

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
    val durationMin: Int,    // 实际学习分钟数
    val distractionCount: Int // 摸鱼次数
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("startMs", startMs)
        put("endMs", endMs)
        put("durationMin", durationMin)
        put("distractionCount", distractionCount)
    }

    companion object {
        fun fromJson(json: JSONObject) = StudySession(
            date = json.getString("date"),
            startMs = json.getLong("startMs"),
            endMs = json.getLong("endMs"),
            durationMin = json.getInt("durationMin"),
            distractionCount = json.getInt("distractionCount")
        )
    }
}

object StudyStatsRepository {

    private const val PREF_KEY = "study_sessions"
    private const val PREF_DISTRACTION = "distraction_count"
    private const val PREF_ACTIVE_START = "active_session_start"
    private const val PREF_ACTIVE_SLOT = "active_session_slot"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getSessions(context: Context): List<StudySession> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { StudySession.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveSessions(context: Context, sessions: List<StudySession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, arr.toString()).apply()
    }

    // 开始一次学习
    fun startSession(context: Context, slotId: Long) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        // 如果已有活跃会话，先结束
        if (prefs.getLong(PREF_ACTIVE_START, 0L) > 0) {
            endSession(context)
        }
        prefs.edit()
            .putLong(PREF_ACTIVE_START, System.currentTimeMillis())
            .putLong(PREF_ACTIVE_SLOT, slotId)
            .putInt(PREF_DISTRACTION, 0)
            .apply()
    }

    // 记录一次摸鱼
    fun recordDistraction(context: Context) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val current = prefs.getInt(PREF_DISTRACTION, 0)
        prefs.edit().putInt(PREF_DISTRACTION, current + 1).apply()
    }

    // 结束当前学习
    fun endSession(context: Context) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val startMs = prefs.getLong(PREF_ACTIVE_START, 0L)
        if (startMs <= 0L) return

        val endMs = System.currentTimeMillis()
        val durationMin = ((endMs - startMs) / 60_000).toInt().coerceAtLeast(0)
        if (durationMin < 1) {
            // 不足1分钟，不记录
            prefs.edit().remove(PREF_ACTIVE_START).remove(PREF_ACTIVE_SLOT).remove(PREF_DISTRACTION).apply()
            return
        }

        val distraction = prefs.getInt(PREF_DISTRACTION, 0)
        val date = dateFormat.format(Date(startMs))

        val session = StudySession(
            date = date,
            startMs = startMs,
            endMs = endMs,
            durationMin = durationMin,
            distractionCount = distraction
        )

        val sessions = getSessions(context).toMutableList()
        sessions.add(session)
        // 只保留最近90天
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -90) }.timeInMillis
        val filtered = sessions.filter { it.endMs > cutoff }
        saveSessions(context, filtered)

        prefs.edit().remove(PREF_ACTIVE_START).remove(PREF_ACTIVE_SLOT).remove(PREF_DISTRACTION).apply()
    }

    // 是否有活跃会话
    fun hasActiveSession(context: Context): Boolean {
        return context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .getLong(PREF_ACTIVE_START, 0L) > 0
    }

    // 获取今日统计
    fun getTodayStats(context: Context): DayStats {
        val today = dateFormat.format(Date())
        val sessions = getSessions(context).filter { it.date == today }
        return aggregate(sessions)
    }

    // 获取本周统计（按天）
    fun getWeekStats(context: Context): List<DayStats> {
        val cal = Calendar.getInstance()
        // 回到本周一
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val monday = dateFormat.format(cal.time)

        val sessions = getSessions(context)
        val result = mutableListOf<DayStats>()
        val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

        for (i in 0 until 7) {
            val date = dateFormat.format(cal.time)
            val daySessions = sessions.filter { it.date == date }
            result.add(DayStats(
                date = date,
                label = dayNames[i],
                totalMinutes = daySessions.sumOf { it.durationMin },
                distractionCount = daySessions.sumOf { it.distractionCount },
                sessionCount = daySessions.size
            ))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    // 获取本月统计（按天）
    fun getMonthStats(context: Context): List<DayStats> {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.set(year, month, 1)

        val sessions = getSessions(context)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = mutableListOf<DayStats>()

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val date = dateFormat.format(cal.time)
            val daySessions = sessions.filter { it.date == date }
            result.add(DayStats(
                date = date,
                label = "$day",
                totalMinutes = daySessions.sumOf { it.durationMin },
                distractionCount = daySessions.sumOf { it.distractionCount },
                sessionCount = daySessions.size
            ))
        }
        return result
    }

    private fun aggregate(sessions: List<StudySession>): DayStats {
        return DayStats(
            date = sessions.firstOrNull()?.date ?: "",
            label = "今日",
            totalMinutes = sessions.sumOf { it.durationMin },
            distractionCount = sessions.sumOf { it.distractionCount },
            sessionCount = sessions.size
        )
    }
}

data class DayStats(
    val date: String,
    val label: String,
    val totalMinutes: Int,
    val distractionCount: Int,
    val sessionCount: Int
)

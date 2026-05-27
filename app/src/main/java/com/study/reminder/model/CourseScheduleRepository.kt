package com.study.reminder.model

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

object CourseScheduleRepository {

    private const val PREF_COURSES = "course_schedules"
    private const val PREF_SEMESTER_START = "semester_start_date"
    private val dateFormatTL = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // ── 课程数据读写 ─────────────────────────────────────────

    fun getCourses(context: Context): List<CourseSchedule> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_COURSES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { CourseSchedule.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { Log.w("CourseScheduleRepo", "Failed to parse courses", e); emptyList() }
    }

    fun saveCourses(context: Context, courses: List<CourseSchedule>) {
        val arr = JSONArray()
        courses.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_COURSES, arr.toString()).apply()
    }

    fun addCourses(context: Context, courses: List<CourseSchedule>) {
        val existing = getCourses(context).toMutableList()
        existing.addAll(courses)
        saveCourses(context, existing)
    }

    fun replaceAllCourses(context: Context, courses: List<CourseSchedule>) {
        saveCourses(context, courses)
    }

    fun removeAllCourses(context: Context) {
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().remove(PREF_COURSES).apply()
    }

    fun toggleCourse(context: Context, id: Long) {
        val courses = getCourses(context).toMutableList()
        val idx = courses.indexOfFirst { it.id == id }
        if (idx >= 0) {
            courses[idx] = courses[idx].copy(enabled = !courses[idx].enabled)
            saveCourses(context, courses)
        }
    }

    fun removeCourse(context: Context, id: Long) {
        saveCourses(context, getCourses(context).filter { it.id != id })
    }

    fun toggleAllCourses(context: Context, enabled: Boolean) {
        val courses = getCourses(context).map { it.copy(enabled = enabled) }
        saveCourses(context, courses)
    }

    // ── 学期起始日 ──────────────────────────────────────────

    fun getSemesterStartDate(context: Context): String {
        return context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .getString(PREF_SEMESTER_START, "") ?: ""
    }

    fun setSemesterStartDate(context: Context, date: String) {
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_SEMESTER_START, date).apply()
    }

    /**
     * 计算当前是学期第几周（从1开始），仅用于显示
     */
    fun getSemesterWeekNumber(context: Context): Int {
        val start = getSemesterStartDate(context)
        if (start.isEmpty()) return 0
        return try {
            val df = dateFormatTL.get()!!
            val startCal = Calendar.getInstance().apply {
                time = df.parse(start)!!
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
            }
            val now = Calendar.getInstance()
            val diffMs = now.timeInMillis - startCal.timeInMillis
            val diffDays = (diffMs / (24 * 60 * 60 * 1000)).toInt()
            (diffDays / 7) + 1
        } catch (_: Exception) { 0 }
    }

    // ── 活跃课程检测 ────────────────────────────────────────

    fun getActiveCoursesNow(context: Context): List<CourseSchedule> {
        return getCourses(context).filter { isCourseActiveNow(context, it) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun isCourseActiveNow(context: Context, course: CourseSchedule): Boolean {
        if (!course.enabled) return false

        val now = Calendar.getInstance()
        val today = dateFormatTL.get()!!.format(now.time)
        val dow = adjustedDayOfWeek(now)
        if (dow != course.dayOfWeek) return false

        // 检查排除日期
        if (today in course.exDates) return false

        // 检查当前时间是否在课程时间内
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMin = course.startHour * 60 + course.startMinute
        val endMin = course.endHour * 60 + course.endMinute
        if (nowMin !in startMin until endMin) return false

        // 检查 weekRanges: today 必须在某个范围内
        if (course.weekRanges.isNotEmpty()) {
            val inRange = course.weekRanges.any { range ->
                today >= range[0] && today <= range[1]
            }
            if (!inRange) return false
        }
        // weekRanges 为空 = 无限制（一直有效）

        return true
    }

    // ── 工具方法 ────────────────────────────────────────────

    private fun adjustedDayOfWeek(cal: Calendar): Int {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 0
        }
    }
}

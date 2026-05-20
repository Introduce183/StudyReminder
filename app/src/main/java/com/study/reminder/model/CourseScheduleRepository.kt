package com.study.reminder.model

import android.content.Context
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

object CourseScheduleRepository {

    private const val PREF_COURSES = "course_schedules"
    private const val PREF_SEMESTER_START = "semester_start_date"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── 课程数据读写 ─────────────────────────────────────────

    fun getCourses(context: Context): List<CourseSchedule> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_COURSES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { CourseSchedule.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
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

    // ── 学期起始日 ──────────────────────────────────────────

    fun getSemesterStartDate(context: Context): String {
        return context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .getString(PREF_SEMESTER_START, "") ?: ""
    }

    fun setSemesterStartDate(context: Context, date: String) {
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_SEMESTER_START, date).apply()
        // 设置学期起始日后，解析隔周课程的单双周
        resolveAllWeekTypes(context)
    }

    /**
     * 设置学期起始日后，对所有隔周课程（courseStartDate 有值且 weekType=ALL）解析单双周
     */
    private fun resolveAllWeekTypes(context: Context) {
        val semesterStart = getSemesterStartDate(context)
        if (semesterStart.isEmpty()) return
        val courses = getCourses(context).toMutableList()
        var changed = false
        for (i in courses.indices) {
            val c = courses[i]
            if (c.biweekly && c.courseStartDate.isNotEmpty() && c.weekType == WeekType.ALL) {
                val courseWeek = weeksBetween(semesterStart, c.courseStartDate)
                val newType = if (courseWeek % 2 == 0) WeekType.ODD else WeekType.EVEN
                if (newType != c.weekType) {
                    courses[i] = c.copy(weekType = newType)
                    changed = true
                }
            }
        }
        if (changed) saveCourses(context, courses)
    }

    /**
     * 计算当前是学期第几周（从1开始）
     */
    fun getSemesterWeekNumber(context: Context): Int {
        val start = getSemesterStartDate(context)
        if (start.isEmpty()) return 0
        return try {
            val startCal = Calendar.getInstance().apply {
                time = dateFormat.parse(start)!!
                // 调整到当周周一
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

    /**
     * 判断当前是否为奇数周（相对学期起始日）
     */
    fun isOddWeek(context: Context): Boolean {
        val weekNum = getSemesterWeekNumber(context)
        return weekNum % 2 == 1
    }

    // ── 活跃课程检测 ────────────────────────────────────────

    fun getActiveCoursesNow(context: Context): List<CourseSchedule> {
        return getCourses(context).filter { isCourseActiveNow(context, it) }
    }

    fun isCourseActiveNow(context: Context, course: CourseSchedule): Boolean {
        if (!course.enabled) return false

        val now = Calendar.getInstance()
        val today = dateFormat.format(now.time)
        val dow = adjustedDayOfWeek(now)
        if (dow != course.dayOfWeek) return false

        // 检查结束日期
        if (course.endDate.isNotEmpty() && today > course.endDate) return false

        // 检查排除日期
        if (today in course.exDates) return false

        // 检查当前时间是否在课程时间内
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMin = course.startHour * 60 + course.startMinute
        val endMin = course.endHour * 60 + course.endMinute
        if (nowMin !in startMin until endMin) return false

        // 检查单双周
        if (course.weekType != WeekType.ALL) {
            val semesterStart = getSemesterStartDate(context)
            if (semesterStart.isEmpty()) return false
            val weeksDiff = weeksBetween(semesterStart, today)
            val isOddWeek = weeksDiff % 2 == 0 // 第0周 = 第1周 = 单周
            if (course.weekType == WeekType.ODD && !isOddWeek) return false
            if (course.weekType == WeekType.EVEN && isOddWeek) return false
        }

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

    private fun weeksBetween(date1Str: String, date2Str: String): Int {
        return try {
            val date1 = dateFormat.parse(date1Str)!!
            val date2 = dateFormat.parse(date2Str)!!
            val cal1 = Calendar.getInstance().apply {
                time = date1
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val cal2 = Calendar.getInstance().apply {
                time = date2
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = cal2.timeInMillis - cal1.timeInMillis
            (diffMs / (7 * 24 * 60 * 60 * 1000)).toInt()
        } catch (_: Exception) { 0 }
    }
}

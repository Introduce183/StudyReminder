package com.study.reminder.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.study.reminder.model.CourseSchedule
import com.study.reminder.model.CourseScheduleRepository
import com.study.reminder.service.CourseAlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object CourseScheduler {

    private const val PREF_SCHEDULED_COURSE_IDS = "scheduled_course_ids"
    private val dateFormatTL = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    private val dateFormat: SimpleDateFormat get() = dateFormatTL.get()!!

    fun rescheduleAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val courses = CourseScheduleRepository.getCourses(context)
        val scheduledIds = loadScheduledIds(context)

        // 取消所有旧闹钟
        scheduledIds.forEach { cancelById(context, am, it) }

        // 重新注册所有启用的课程
        courses.filter { it.enabled }.forEach { course ->
            scheduleCourse(context, am, course)
        }
        saveScheduledIds(context, courses.filter { it.enabled }.map { it.id }.toSet())
    }

    fun scheduleCourse(context: Context, am: AlarmManager, course: CourseSchedule) {
        val triggerTime = nextTriggerMillis(course) ?: return

        val intent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = CourseAlarmReceiver.ACTION_COURSE_TRIGGER
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, course.id)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, course.courseName)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_LOCATION, course.location)
        }
        val pi = PendingIntent.getBroadcast(
            context, courseRequestCode(course), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        }
    }

    private fun cancelById(context: Context, am: AlarmManager, courseId: Long) {
        val intent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = CourseAlarmReceiver.ACTION_COURSE_TRIGGER
        }
        val pi = PendingIntent.getBroadcast(
            context, courseRequestCode(courseId), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun courseRequestCode(course: CourseSchedule): Int = courseRequestCode(course.id)
    private fun courseRequestCode(courseId: Long): Int = ((courseId xor (courseId ushr 32)).toInt()) xor 0x7A1B

    private fun loadScheduledIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet(PREF_SCHEDULED_COURSE_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    private fun saveScheduledIds(context: Context, ids: Set<Long>) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(PREF_SCHEDULED_COURSE_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    /**
     * 计算课程下次触发时间
     * 向前搜索 14 天
     */
    private fun nextTriggerMillis(course: CourseSchedule): Long? {
        val now = Calendar.getInstance()
        val today = dateFormat.format(now.time)

        // 如果 weekRanges 非空，检查是否还有未过期的范围
        if (course.weekRanges.isNotEmpty()) {
            val lastEnd = course.weekRanges.maxOf { it[1] }
            if (today > lastEnd) return null
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, course.startHour)
            set(Calendar.MINUTE, course.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(14) { offset ->
            val candidate = cal.clone() as Calendar
            candidate.add(Calendar.DAY_OF_YEAR, offset)
            val candidateDate = dateFormat.format(candidate.time)

            // 检查排除日期
            if (candidateDate in course.exDates) return@repeat

            // 检查星期
            val dow = candidate.get(Calendar.DAY_OF_WEEK)
            val adjustedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1
            if (adjustedDow != course.dayOfWeek) return@repeat

            // 检查 weekRanges
            if (course.weekRanges.isNotEmpty()) {
                val inRange = course.weekRanges.any { range ->
                    candidateDate >= range[0] && candidateDate <= range[1]
                }
                if (!inRange) return@repeat
            }

            if (candidate.after(now)) {
                return candidate.timeInMillis
            }
        }
        return null
    }
}

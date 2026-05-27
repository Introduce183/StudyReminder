package com.study.reminder.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.study.reminder.helper.CourseScheduler
import com.study.reminder.helper.ReminderDispatcher
import com.study.reminder.model.CourseScheduleRepository
import java.text.SimpleDateFormat
import java.util.*

class CourseAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COURSE_TRIGGER = "com.study.reminder.COURSE_TRIGGER"
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_LOCATION = "course_location"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COURSE_TRIGGER) return

        val courseId = intent.getLongExtra(EXTRA_COURSE_ID, -1L)
        val courses = CourseScheduleRepository.getCourses(context)
        val course = courses.find { it.id == courseId } ?: return

        // 检查课程是否仍然有效
        if (!course.enabled) return

        val now = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        if (today in course.exDates) return

        // 检查今天是否是课程的星期
        val dow = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7; else -> 0
        }
        if (dow != course.dayOfWeek) return

        // 检查 weekRanges
        if (course.weekRanges.isNotEmpty()) {
            val inRange = course.weekRanges.any { range ->
                today >= range[0] && today <= range[1]
            }
            if (!inRange) return
        }

        // 发送提醒
        val locationInfo = if (course.location.isNotEmpty()) " @ ${course.location}" else ""
        val message = "课程「${course.courseName}」${locationInfo} 开始了！"
        ReminderDispatcher.dispatchFromPreferences(context, message, "📖")

        // 重新调度下次触发
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        CourseScheduler.scheduleCourse(context, am, course)
    }
}

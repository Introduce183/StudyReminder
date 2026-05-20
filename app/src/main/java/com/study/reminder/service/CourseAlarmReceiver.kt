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
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (!course.enabled) return
        if (course.endDate.isNotEmpty() && today > course.endDate) return
        if (today in course.exDates) return

        // 检查单双周
        if (course.weekType != com.study.reminder.model.WeekType.ALL) {
            val semesterStart = CourseScheduleRepository.getSemesterStartDate(context)
            if (semesterStart.isNotEmpty()) {
                val weeksDiff = weeksBetween(semesterStart, today)
                val isOddWeek = weeksDiff % 2 == 0
                if (course.weekType == com.study.reminder.model.WeekType.ODD && !isOddWeek) return
                if (course.weekType == com.study.reminder.model.WeekType.EVEN && isOddWeek) return
            }
        }

        // 发送提醒
        val locationInfo = if (course.location.isNotEmpty()) " @ ${course.location}" else ""
        val message = "课程「${course.courseName}」${locationInfo} 开始了！"
        ReminderDispatcher.dispatchFromPreferences(context, message, "📖")

        // 重新调度下次触发
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        CourseScheduler.scheduleCourse(context, am, course)
    }

    private fun weeksBetween(date1Str: String, date2Str: String): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val date1 = fmt.parse(date1Str)!!
            val date2 = fmt.parse(date2Str)!!
            val cal1 = Calendar.getInstance().apply {
                time = date1
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val cal2 = Calendar.getInstance().apply {
                time = date2
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            ((cal2.timeInMillis - cal1.timeInMillis) / (7 * 24 * 60 * 60 * 1000)).toInt()
        } catch (_: Exception) { 0 }
    }
}

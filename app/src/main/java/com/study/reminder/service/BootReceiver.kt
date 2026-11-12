package com.study.reminder.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.study.reminder.helper.CourseScheduler
import com.study.reminder.helper.SlotScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                SlotScheduler.rescheduleAll(context)
                CourseScheduler.rescheduleAll(context)
                // 开机后恢复保活前台服务（用户可在设置中关闭）
                val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("opt_keep_alive", true)) {
                    KeepAliveService.start(context)
                }
            }

            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    SlotScheduler.rescheduleAll(context)
                    CourseScheduler.rescheduleAll(context)
                    return
                }

                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager.canScheduleExactAlarms()) {
                    SlotScheduler.rescheduleAll(context)
                    CourseScheduler.rescheduleAll(context)
                }
            }
        }
    }
}

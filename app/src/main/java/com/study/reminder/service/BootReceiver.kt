package com.study.reminder.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.study.reminder.helper.SlotScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                SlotScheduler.rescheduleAll(context)
            }

            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    SlotScheduler.rescheduleAll(context)
                    return
                }

                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager.canScheduleExactAlarms()) {
                    SlotScheduler.rescheduleAll(context)
                }
            }
        }
    }
}

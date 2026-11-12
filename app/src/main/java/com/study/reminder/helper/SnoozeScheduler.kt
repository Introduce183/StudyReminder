package com.study.reminder.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.study.reminder.service.SnoozeReceiver

/**
 * 「再看 30 秒」：延迟一段时间后重新触发全屏提醒。
 * 在这段时间内暂停无障碍监督，避免点完立刻又被弹窗。
 */
object SnoozeScheduler {

    const val ACTION_SNOOZE = "com.study.reminder.SNOOZE_TRIGGER"
    const val DEFAULT_DELAY_SECONDS = 30
    private const val REQUEST_CODE = 7001

    @Volatile private var snoozeUntilElapsed = 0L

    fun isSnoozing(): Boolean = SystemClock.elapsedRealtime() < snoozeUntilElapsed

    fun onTriggered() {
        snoozeUntilElapsed = 0L
    }

    fun schedule(context: Context, delaySeconds: Int = DEFAULT_DELAY_SECONDS) {
        snoozeUntilElapsed = SystemClock.elapsedRealtime() + delaySeconds * 1000L
        val pending = pendingIntent(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        snoozeUntilElapsed = 0L
        val pending = pendingIntent(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

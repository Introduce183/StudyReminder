package com.study.reminder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.SlotRepository

/**
 * 时间段闹钟触发时的广播接收器
 * 时间到 → 启动TimerService运行倒计时（时长 = 时间段duration）
 */
class SlotAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SLOT_TRIGGER = "com.study.reminder.SLOT_TRIGGER"
        const val EXTRA_SLOT_ID = "slot_id"
        const val EXTRA_SLOT_LABEL = "slot_label"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SLOT_TRIGGER) return

        val slotId = intent.getLongExtra(EXTRA_SLOT_ID, -1L)
        val slots = SlotRepository.getSlots(context)
        val slot = slots.find { it.id == slotId } ?: return

        // 计算时间段时长（分钟）
        val durationMinutes = (slot.endHour * 60 + slot.endMinute) -
                              (slot.startHour * 60 + slot.startMinute)
        if (durationMinutes <= 0) return

        // 读取提醒设置
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(TimerService.EXTRA_ENABLE_NOTIFICATION, prefs.getBoolean("opt_notification", true))
            putExtra(TimerService.EXTRA_ENABLE_DYNAMIC_ISLAND, prefs.getBoolean("opt_island", false))
            putExtra(TimerService.EXTRA_ENABLE_SMALL_WINDOW, prefs.getBoolean("opt_small_window", false))
            putExtra(TimerService.EXTRA_ENABLE_FULLSCREEN, prefs.getBoolean("opt_fullscreen", false))
            putExtra(TimerService.EXTRA_ENABLE_FORCE_HOME, prefs.getBoolean("opt_force_home", false))
            putExtra(TimerService.EXTRA_SLOT_LABEL, slot.label)
        }
        context.startForegroundService(serviceIntent)

        // 重新调度这个时间段下次触发（每周循环）
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        SlotScheduler.scheduleSlot(context, am, slot)
    }
}

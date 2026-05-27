package com.study.reminder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import com.study.reminder.helper.ReminderDispatcher
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.SlotRepository

/**
 * 时间段闹钟触发时的广播接收器
 * 时间到 → 发送提醒通知（不自动开始计时，由用户手动开始或监督触发）
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
        if (!slot.enabled) return

        // 发送提醒通知
        val message = "学习时间段「${slot.label.ifEmpty { "${slot.startLabel} - ${slot.endLabel}" }}」到了！"
        val emoji = "📚"
        ReminderDispatcher.dispatchFromPreferences(context, message, emoji)

        // 重新调度这个时间段下次触发（每周循环）
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        SlotScheduler.scheduleSlot(context, am, slot)
    }
}

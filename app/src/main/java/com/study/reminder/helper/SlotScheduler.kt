package com.study.reminder.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudySlot
import com.study.reminder.service.SlotAlarmReceiver
import java.util.Calendar

object SlotScheduler {

    private const val PREF_SCHEDULED_SLOT_IDS = "scheduled_slot_ids"

    /**
     * 重新调度所有已启用的时间段
     * 每次增删或修改时间段后调用
     */
    fun rescheduleAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val slots = SlotRepository.getSlots(context)
        val scheduledIds = loadScheduledSlotIds(context)

        // 先取消所有旧闹钟，包括已经被删除的时间段
        scheduledIds.forEach { cancelSlotById(context, am, it) }

        // 重新注册所有启用的
        slots.filter { it.enabled }.forEach { slot ->
            scheduleSlot(context, am, slot)
        }
        saveScheduledSlotIds(context, slots.filter { it.enabled }.map { it.id }.toSet())
    }

    fun scheduleSlot(context: Context, am: AlarmManager, slot: StudySlot) {
        val triggerTime = nextTriggerMillis(slot) ?: return

        val intent = Intent(context, SlotAlarmReceiver::class.java).apply {
            action = SlotAlarmReceiver.ACTION_SLOT_TRIGGER
            putExtra(SlotAlarmReceiver.EXTRA_SLOT_ID, slot.id)
            putExtra(SlotAlarmReceiver.EXTRA_SLOT_LABEL, slot.label)
        }
        val pi = PendingIntent.getBroadcast(
            context, slotRequestCode(slot), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        }
    }

    fun cancelSlot(context: Context, am: AlarmManager, slot: StudySlot) {
        cancelSlotById(context, am, slot.id)
    }

    private fun cancelSlotById(context: Context, am: AlarmManager, slotId: Long) {
        val intent = Intent(context, SlotAlarmReceiver::class.java).apply {
            action = SlotAlarmReceiver.ACTION_SLOT_TRIGGER
        }
        val pi = PendingIntent.getBroadcast(
            context, slotRequestCode(slotId), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun slotRequestCode(slot: StudySlot): Int = slotRequestCode(slot.id)

    private fun slotRequestCode(slotId: Long): Int = (slotId xor (slotId ushr 32)).toInt()

    private fun loadScheduledSlotIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet(PREF_SCHEDULED_SLOT_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    private fun saveScheduledSlotIds(context: Context, slotIds: Set<Long>) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(PREF_SCHEDULED_SLOT_IDS, slotIds.map { it.toString() }.toSet())
            .apply()
    }

    /**
     * 计算时间段下次触发的毫秒时间戳
     * 若今天这个时间段还没到，就今天触发；否则找下一个启用的星期
     */
    private fun nextTriggerMillis(slot: StudySlot): Long? {
        if (slot.enableDays.isEmpty()) return null
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, slot.startHour)
            set(Calendar.MINUTE, slot.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 最多往后找7天
        repeat(7) { offset ->
            val candidate = cal.clone() as Calendar
            candidate.add(Calendar.DAY_OF_YEAR, offset)
            val dow = candidate.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
            val adjustedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1 // 转成1=Mon..7=Sun
            if (adjustedDow in slot.enableDays && candidate.after(now)) {
                return candidate.timeInMillis
            }
        }
        return null
    }
}

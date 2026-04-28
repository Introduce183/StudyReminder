package com.study.reminder.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import com.study.reminder.helper.ReminderDispatcher
import android.view.accessibility.AccessibilityEvent
import com.study.reminder.helper.WhitelistManager
import com.study.reminder.model.SlotRepository
import java.util.Calendar

/**
 * 无障碍服务：仅用于检测App切换，实现学习中途开手机提醒
 * 核心时间段提醒（AlarmManager）完全不依赖此服务
 */
class ScreenAccessibilityService : AccessibilityService() {

    companion object {
        var lastPackageName: String = ""
        private var lastReminderPkg: String = ""
        private var lastReminderTime: Long = 0L
        private const val COOLDOWN_MS = 5 * 60 * 1000L
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            // 只监听窗口切换，不读内容，降低性能消耗
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 500
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == lastPackageName) return
        lastPackageName = pkg
        checkAndRemind(pkg)
    }

    private fun checkAndRemind(pkg: String) {
        if (!isInStudyTime()) return
        if (WhitelistManager.isWhitelisted(this, pkg)) return
        val now = System.currentTimeMillis()
        if (pkg == lastReminderPkg && now - lastReminderTime < COOLDOWN_MS) return

        lastReminderPkg = pkg
        lastReminderTime = now

        val (message, emoji) = com.study.reminder.helper.TauntHelper.random()
        ReminderDispatcher.dispatchFromPreferences(this, message, emoji)
    }

    private fun isInStudyTime(): Boolean {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val dow = now.get(Calendar.DAY_OF_WEEK)
        val adjustedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1

        return SlotRepository.getSlots(this).any { slot ->
            if (!slot.enabled) return@any false
            if (adjustedDow !in slot.enableDays) return@any false
            val startMin = slot.startHour * 60 + slot.startMinute
            val endMin = slot.endHour * 60 + slot.endMinute
            nowMinutes in startMin until endMin
        }
    }

    override fun onInterrupt() {}
}

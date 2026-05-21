package com.study.reminder.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import com.study.reminder.helper.ReminderDispatcher
import android.view.accessibility.AccessibilityEvent
import com.study.reminder.helper.WhitelistManager
import com.study.reminder.model.CourseScheduleRepository
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudyStatsRepository
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

    private val launcherPkgs = mutableSetOf<String>()

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 500
        }
        cacheLauncherPackages()
    }

    private fun cacheLauncherPackages() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolved = packageManager.queryIntentActivities(intent, 0)
            launcherPkgs.clear()
            launcherPkgs.addAll(resolved.map { it.activityInfo.packageName })
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == lastPackageName) return
        lastPackageName = pkg
        // Refresh cache if package unknown (newly installed app)
        if (pkg !in launcherPkgs) {
            cacheLauncherPackages()
        }
        if (pkg !in launcherPkgs) return
        if (pkg == packageName) return
        checkAndRemind(pkg)
    }

    private fun checkAndRemind(pkg: String) {
        if (!isInStudyTime()) return
        if (WhitelistManager.isWhitelisted(this, pkg)) return
        val now = System.currentTimeMillis()
        if (pkg == lastReminderPkg && now - lastReminderTime < COOLDOWN_MS) return

        lastReminderPkg = pkg
        lastReminderTime = now

        // 监督触发时自动开始学习计时（如果尚未开始）
        if (!StudyStatsRepository.hasActiveSession(this)) {
            StudyStatsRepository.startSession(this, -1L)
            val remaining = StudyStatsRepository.getActiveSlotRemainingMinutes(this)
            if (remaining > 0) {
                val serviceIntent = android.content.Intent(this, com.study.reminder.service.TimerService::class.java).apply {
                    action = TimerService.ACTION_START
                    putExtra(TimerService.EXTRA_DURATION_MINUTES, remaining)
                }
                startForegroundService(serviceIntent)
            }
        }

        val (message, emoji) = com.study.reminder.helper.TauntHelper.random()
        ReminderDispatcher.dispatchFromPreferences(this, message, emoji)
    }

    private fun isInStudyTime(): Boolean {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val dow = now.get(Calendar.DAY_OF_WEEK)
        val adjustedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1

        // 检查手动时间段
        val inManualSlot = SlotRepository.getSlots(this).any { slot ->
            if (!slot.enabled) return@any false
            if (adjustedDow !in slot.enableDays) return@any false
            val startMin = slot.startHour * 60 + slot.startMinute
            val endMin = slot.endHour * 60 + slot.endMinute
            nowMinutes in startMin until endMin
        }
        if (inManualSlot) return true

        // 检查课表
        return CourseScheduleRepository.getActiveCoursesNow(this).isNotEmpty()
    }

    override fun onInterrupt() {}
}

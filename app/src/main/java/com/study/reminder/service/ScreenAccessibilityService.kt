package com.study.reminder.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import com.study.reminder.helper.ReminderDispatcher
import android.view.accessibility.AccessibilityEvent
import com.study.reminder.helper.SnoozeScheduler
import com.study.reminder.helper.StudyTimeCache
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
        private var lastEventTime: Long = 0L
        private var lastReminderPkg: String = ""
        private var lastReminderTime: Long = 0L
        /** 停留在同一个分心应用内时的重复提醒冷却 */
        private const val COOLDOWN_MS = 5 * 60 * 1000L
        /** 同一包名连续事件的去重窗口（避免窗口切换事件风暴） */
        private const val SAME_PACKAGE_DEDUPE_MS = 1_500L

        private const val STUDY_TIME_CACHE_TTL = 10_000L
    }

    private val launcherPkgs = mutableSetOf<String>()
    private var cachedWhitelist: Set<String>? = null
    private var whitelistCacheTime: Long = 0L
    private val WHITELIST_CACHE_TTL = 60_000L

    override fun onServiceConnected() {
        // 在 XML 配置的基础上追加窗口监听能力：
        // 部分游戏 / 全屏应用不会稳定发送 TYPE_WINDOW_STATE_CHANGED
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 500
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
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
        if (event == null) return
        // 部分游戏/全屏窗口的包名为空，退回到当前活动窗口的包名
        val pkg = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: return
        val now = android.os.SystemClock.elapsedRealtime()

        // 仅对短时间内的同一包名重复事件去重，不能永久拦截
        if (pkg == lastPackageName && now - lastEventTime < SAME_PACKAGE_DEDUPE_MS) return
        lastPackageName = pkg
        lastEventTime = now

        // Refresh cache if package unknown (newly installed app)
        if (pkg !in launcherPkgs) {
            cacheLauncherPackages()
        }
        // 离开受监控的应用（最近任务、系统界面等）也算“离开”，重置同应用冷却。
        // 仅对真正的窗口切换生效，输入法 / 系统弹窗的窗口变化不重置。
        if (pkg !in launcherPkgs) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                resetAppReminderGuard()
            }
            return
        }
        if (pkg == packageName) {
            // 只有真正打开主界面才重置冷却；
            // 悬浮窗 / 提醒 Activity 不能被当成“离开分心应用”
            if (event.className?.toString() ==
                com.study.reminder.ui.MainActivity::class.java.name
            ) {
                resetAppReminderGuard()
            }
            return
        }
        checkAndRemind(pkg)
    }

    /** 用户离开分心应用后重置冷却，下次再进入同一应用应重新提醒 */
    private fun resetAppReminderGuard() {
        lastReminderPkg = ""
        lastReminderTime = 0L
    }

    private fun isWhitelistedCached(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (cachedWhitelist == null || now - whitelistCacheTime > WHITELIST_CACHE_TTL) {
            cachedWhitelist = WhitelistManager.getWhitelist(this)
            whitelistCacheTime = now
        }
        return pkg in cachedWhitelist!!
    }

    private fun checkAndRemind(pkg: String) {
        if (!isInStudyTime()) return
        if (isWhitelistedCached(pkg)) {
            // 回到白名单应用（含桌面）：视为离开分心应用，重置冷却
            resetAppReminderGuard()
            return
        }

        // 「再看 30 秒」期间暂停无障碍监督，避免点完立刻又被弹窗
        if (SnoozeScheduler.isSnoozing()) return

        // 以实时数据为准：没有启用中的时间段 / 进行中的课程 /
        // 手动开始的学习会话时，不提醒、也不启动计时。
        val remaining = StudyStatsRepository.getActiveSlotRemainingMinutes(this)
        val hasSession = StudyStatsRepository.hasActiveSession(this)
        if (remaining <= 0 && !hasSession) {
            StudyTimeCache.invalidate()
            return
        }

        val now = System.currentTimeMillis()
        // 仅拦截停留在同一分心应用内的重复提醒
        if (pkg == lastReminderPkg && now - lastReminderTime < COOLDOWN_MS) return

        lastReminderPkg = pkg
        lastReminderTime = now

        // 监督触发时自动开始学习计时（如果尚未开始）
        if (!hasSession) {
            val activeSlotId = StudyStatsRepository.getActiveSlotId(this)
            val source = when {
                activeSlotId > 0L -> StudyStatsRepository.SOURCE_SLOT
                CourseScheduleRepository.getActiveCoursesNow(this).isNotEmpty() ->
                    StudyStatsRepository.SOURCE_COURSE
                else -> StudyStatsRepository.SOURCE_AUTO
            }
            StudyStatsRepository.startSession(this, activeSlotId, source)
            val serviceIntent = android.content.Intent(this, com.study.reminder.service.TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_DURATION_MINUTES, remaining)
            }
            startForegroundService(serviceIntent)
        }

        val (message, emoji) = com.study.reminder.helper.TauntHelper.random()
        ReminderDispatcher.dispatchFromPreferences(this, message, emoji, recordDistraction = true)
    }

    private fun isInStudyTime(): Boolean {
        val now = System.currentTimeMillis()
        StudyTimeCache.cachedInStudyTime?.let {
            if (now - StudyTimeCache.cacheTime < STUDY_TIME_CACHE_TTL) return it
        }
        val result = isInStudyTimeUncached()
        StudyTimeCache.cachedInStudyTime = result
        StudyTimeCache.cacheTime = now
        return result
    }

    private fun isInStudyTimeUncached(): Boolean {
        // 手动开始的学习会话：即使不在固定时间段内也应持续监督
        if (StudyStatsRepository.hasActiveSession(this)) return true

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

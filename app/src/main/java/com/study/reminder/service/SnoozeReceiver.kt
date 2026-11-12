package com.study.reminder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.study.reminder.helper.ReminderDispatcher
import com.study.reminder.helper.SnoozeScheduler
import com.study.reminder.helper.TauntHelper
import com.study.reminder.helper.WhitelistManager
import com.study.reminder.model.StudyStatsRepository

/**
 * 「再看 30 秒」到点后重新触发一次提醒。
 * 如果用户已经结束学习或不在监督时段，则不再打扰。
 */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SnoozeScheduler.ACTION_SNOOZE) return
        SnoozeScheduler.onTriggered()

        val stillStudying = StudyStatsRepository.hasActiveSession(context) ||
            StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0
        if (!stillStudying) return

        // 如果用户已经回到本应用或白名单应用，不再打扰
        val lastPkg = ScreenAccessibilityService.lastPackageName
        if (lastPkg.isNotEmpty()) {
            if (lastPkg == context.packageName) return
            if (WhitelistManager.isWhitelisted(context, lastPkg)) return
        }

        val (message, emoji) = TauntHelper.random()
        ReminderDispatcher.dispatchFromPreferences(
            context = context,
            message = message,
            emoji = emoji,
            recordDistraction = true
        )
    }
}

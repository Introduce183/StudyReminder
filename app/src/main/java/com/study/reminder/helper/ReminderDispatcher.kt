package com.study.reminder.helper

import android.content.Context
import android.content.Intent
import com.study.reminder.service.OverlayService

object ReminderDispatcher {

    fun dispatchFromPreferences(context: Context, message: String, emoji: String) {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        dispatch(
            context = context,
            message = message,
            emoji = emoji,
            enNotif = prefs.getBoolean("opt_notification", true),
            enIsland = prefs.getBoolean("opt_island", false),
            enSmall = prefs.getBoolean("opt_small_window", false),
            enFull = prefs.getBoolean("opt_fullscreen", false),
            enForce = prefs.getBoolean("opt_force_home", false)
        )
    }

    fun dispatch(
        context: Context,
        message: String,
        emoji: String,
        enNotif: Boolean,
        enIsland: Boolean,
        enSmall: Boolean,
        enFull: Boolean,
        enForce: Boolean
    ) {
        val requestFullScreen = enFull || enForce
        val shouldNotify = enNotif || requestFullScreen

        if (shouldNotify) {
            NotificationHelper.showRepeatingAlertNotification(
                context = context,
                message = message,
                emoji = emoji,
                requestFullScreen = requestFullScreen,
                goHomeOnShow = enForce
            )
        }

        if (enIsland) {
            runCatching {
                context.startService(Intent(context, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW_DYNAMIC_ISLAND
                    putExtra(OverlayService.EXTRA_MESSAGE, message)
                    putExtra(OverlayService.EXTRA_EMOJI, emoji)
                })
            }
        }

        if (enSmall) {
            runCatching {
                context.startService(Intent(context, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW_SMALL_WINDOW
                    putExtra(OverlayService.EXTRA_MESSAGE, message)
                    putExtra(OverlayService.EXTRA_EMOJI, emoji)
                })
            }
        }
    }
}

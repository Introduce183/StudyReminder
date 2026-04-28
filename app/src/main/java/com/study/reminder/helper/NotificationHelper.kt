package com.study.reminder.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.study.reminder.R
import com.study.reminder.ui.FullScreenAlertActivity
import com.study.reminder.ui.MainActivity

object NotificationHelper {

    const val FOREGROUND_ID = 1001
    const val ALERT_ID = 2001
    private const val CHANNEL_FOREGROUND = "ch_foreground"
    private const val CHANNEL_ALERT = "ch_alert"
    private const val REQUEST_MAIN = 3001
    private const val REQUEST_ALERT = 3002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOREGROUND,
                "Study timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground countdown for the active study slot."
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                "Study alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent reminder shown when a study slot starts or is interrupted."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 300)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    fun buildForegroundNotification(context: Context, remainingSeconds: Int): Notification {
        val h = remainingSeconds / 3600
        val m = (remainingSeconds % 3600) / 60
        val s = remainingSeconds % 60
        val timeStr = String.format("%02d:%02d:%02d", h, m, s)

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_MAIN,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Study timer running")
            .setContentText("Remaining: $timeStr")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }

    fun updateForegroundNotification(context: Context, timeStr: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_MAIN,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Study timer running")
            .setContentText("Remaining: $timeStr")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
        nm.notify(FOREGROUND_ID, notification)
    }

    fun showRepeatingAlertNotification(
        context: Context,
        message: String,
        emoji: String,
        requestFullScreen: Boolean = false,
        goHomeOnShow: Boolean = false
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val handler = Handler(Looper.getMainLooper())
        val totalRepeat = 6
        var count = 0

        val runnable = object : Runnable {
            override fun run() {
                if (count >= totalRepeat) {
                    handler.postDelayed({ nm.cancel(ALERT_ID) }, 30_000L)
                    return
                }

                val notification = buildAlertNotification(
                    context = context,
                    message = message,
                    emoji = emoji,
                    count = count,
                    enableFullScreen = requestFullScreen && count == 0,
                    goHomeOnShow = goHomeOnShow
                )
                nm.notify(ALERT_ID, notification)
                count++
                handler.postDelayed(this, 2000L)
            }
        }
        handler.post(runnable)
    }

    private fun buildAlertNotification(
        context: Context,
        message: String,
        emoji: String,
        count: Int,
        enableFullScreen: Boolean,
        goHomeOnShow: Boolean
    ): Notification {
        val titles = listOf(
            "$emoji Time to study",
            "$emoji Back to focus",
            "$emoji Study slot started",
            "$emoji Put the phone down",
            "$emoji Focus mode now",
            "$emoji Final reminder"
        )
        val title = titles.getOrElse(count) { titles.last() }

        val alertIntent = FullScreenAlertActivity.createIntent(
            context = context,
            message = message,
            emoji = emoji,
            goHomeOnShow = goHomeOnShow
        )
        val alertPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_ALERT,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(alertPendingIntent)

        if (enableFullScreen && NotificationManagerCompat.from(context).canUseFullScreenIntent()) {
            builder.setFullScreenIntent(alertPendingIntent, true)
        }

        return builder.build()
    }

    fun cancelAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ALERT_ID)
        nm.cancel(FOREGROUND_ID)
    }

    fun cancelForeground(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(FOREGROUND_ID)
    }
}

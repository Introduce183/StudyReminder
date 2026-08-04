package com.study.reminder.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.study.reminder.R
import com.study.reminder.helper.NotificationHelper

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW_DYNAMIC_ISLAND = "show_dynamic_island"
        const val ACTION_SHOW_FULLSCREEN_ALERT = "show_fullscreen_alert"
        const val ACTION_DISMISS = "dismiss"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_EMOJI = "emoji"
        const val EXTRA_CHARACTER = "character"
        const val EXTRA_GO_HOME_ON_SHOW = "go_home_on_show"
    }

    private var windowManager: WindowManager? = null
    private var dynamicIslandView: View? = null
    private var fullscreenView: View? = null
    @Volatile private var isRemovingIsland = false
    @Volatile private var isRemovingFullscreen = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_DYNAMIC_ISLAND -> {
                val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: "该学习了"
                val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "!"
                showDynamicIsland(msg, emoji)
            }

            ACTION_SHOW_FULLSCREEN_ALERT -> {
                val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: "该学习了"
                val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "!"
                val goHomeOnShow = intent.getBooleanExtra(EXTRA_GO_HOME_ON_SHOW, false)
                showFullscreenAlert(msg, emoji, goHomeOnShow)
            }

            ACTION_DISMISS -> {
                removeAllOverlays()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showDynamicIsland(message: String, emoji: String) {
        if (!ensureOverlayPermission(message, emoji)) return

        removeDynamicIsland()
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_dynamic_island, null)

        view.findViewById<TextView>(R.id.tv_island_emoji).text = emoji
        view.findViewById<TextView>(R.id.tv_island_message).text = message

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 48
        }

        val added = runCatching {
            windowManager?.addView(view, params)
        }.isSuccess
        if (!added) {
            NotificationHelper.showRepeatingAlertNotification(this, message, emoji)
            stopSelf()
            return
        }

        dynamicIslandView = view
        view.setOnClickListener { dismissIslandWithAnimation() }

        // Spring-like entrance: scale from 0.95 with opacity
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.alpha = 0f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
            .start()

        handler.postDelayed({ dismissIslandWithAnimation() }, 8000L)
    }

    private fun dismissIslandWithAnimation() {
        val view = dynamicIslandView ?: return
        // Quick exit: scale to 0.95 with opacity
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateInterpolator(2f))
            .withEndAction { removeDynamicIsland() }
            .start()
    }

    private fun removeDynamicIsland() {
        if (isRemovingIsland) return
        isRemovingIsland = true
        dynamicIslandView?.let {
            runCatching { windowManager?.removeView(it) }
        }
        dynamicIslandView = null
        isRemovingIsland = false
    }

    private fun showFullscreenAlert(message: String, emoji: String, goHomeOnShow: Boolean) {
        if (!ensureOverlayPermission(message, emoji)) {
            if (goHomeOnShow) goHome()
            return
        }

        removeFullscreenAlert()
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_fullscreen_alert, null)

        view.findViewById<TextView>(R.id.tv_overlay_message).text = message
        view.findViewById<TextView>(R.id.tv_overlay_emoji).text = emoji

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val added = runCatching {
            windowManager?.addView(view, params)
        }.isSuccess
        if (!added) {
            if (goHomeOnShow) goHome()
            NotificationHelper.showRepeatingAlertNotification(this, message, emoji)
            stopSelf()
            return
        }

        fullscreenView = view
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(220L).start()

        val overlayConfirmButton = view.findViewById<View>(R.id.btn_overlay_confirm)
        overlayConfirmButton.setOnClickListener {
            removeFullscreenAlert()
            goHome()
            stopSelf()
        }

        val autoDismissDelay = if (goHomeOnShow) 1200L else 10_000L
        handler.postDelayed({
            removeFullscreenAlert()
            if (goHomeOnShow) goHome()
            stopSelf()
        }, autoDismissDelay)
    }

    private fun ensureOverlayPermission(message: String, emoji: String): Boolean {
        if (Settings.canDrawOverlays(this)) return true

        NotificationHelper.showRepeatingAlertNotification(this, message, emoji)
        stopSelf()
        return false
    }

    private fun removeFullscreenAlert() {
        if (isRemovingFullscreen) return
        isRemovingFullscreen = true
        fullscreenView?.let {
            runCatching { windowManager?.removeView(it) }
        }
        fullscreenView = null
        isRemovingFullscreen = false
    }

    private fun removeAllOverlays() {
        removeDynamicIsland()
        removeFullscreenAlert()
    }

    private fun goHome() {
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }

    override fun onDestroy() {
        removeAllOverlays()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

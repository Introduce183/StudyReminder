package com.study.reminder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import com.study.reminder.helper.NotificationHelper
import com.study.reminder.helper.ReminderDispatcher
import com.study.reminder.helper.TauntHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerService : Service() {

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val ACTION_MID_REMIND = "action_mid_remind"

        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_ENABLE_NOTIFICATION = "enable_notification"
        const val EXTRA_ENABLE_DYNAMIC_ISLAND = "enable_dynamic_island"
        const val EXTRA_ENABLE_SMALL_WINDOW = "enable_small_window"
        const val EXTRA_ENABLE_FULLSCREEN = "enable_fullscreen"
        const val EXTRA_ENABLE_FORCE_HOME = "enable_force_home"
        const val EXTRA_SLOT_LABEL = "slot_label"

        val remainingTimeLive = MutableLiveData(0)
        val timerRunningLive = MutableLiveData(false)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null
    private var remainingSeconds = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("study_prefs", Context.MODE_PRIVATE)

        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                startTimer(minutes, prefs)
            }

            ACTION_STOP -> stopTimer()

            ACTION_MID_REMIND -> {
                val (message, emoji) = TauntHelper.random()
                serviceScope.launch {
                    withContext(Dispatchers.Main) {
                        ReminderDispatcher.dispatch(
                            message = message,
                            emoji = emoji,
                            context = this@TimerService,
                            enNotif = prefs.getBoolean("opt_notification", true),
                            enIsland = prefs.getBoolean("opt_island", false),
                            enSmall = prefs.getBoolean("opt_small_window", false),
                            enFull = prefs.getBoolean("opt_fullscreen", false),
                            enForce = prefs.getBoolean("opt_force_home", false)
                        )
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startTimer(minutes: Int, prefs: android.content.SharedPreferences) {
        remainingSeconds = minutes * 60
        timerRunningLive.postValue(true)

        startForegroundCompat(
            NotificationHelper.FOREGROUND_ID,
            NotificationHelper.buildForegroundNotification(this, remainingSeconds)
        )

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                remainingTimeLive.postValue(remainingSeconds)
                val h = remainingSeconds / 3600
                val m = (remainingSeconds % 3600) / 60
                val s = remainingSeconds % 60
                NotificationHelper.updateForegroundNotification(
                    this@TimerService,
                    String.format("%02d:%02d:%02d", h, m, s)
                )
            }
            onTimerFinished(prefs)
        }
    }

    private suspend fun onTimerFinished(prefs: android.content.SharedPreferences) {
        timerRunningLive.postValue(false)
        val (message, emoji) = TauntHelper.random()

        withContext(Dispatchers.Main) {
            ReminderDispatcher.dispatch(
                message = message,
                emoji = emoji,
                context = this@TimerService,
                enNotif = prefs.getBoolean("opt_notification", true),
                enIsland = prefs.getBoolean("opt_island", false),
                enSmall = prefs.getBoolean("opt_small_window", false),
                enFull = prefs.getBoolean("opt_fullscreen", false),
                enForce = prefs.getBoolean("opt_force_home", false)
            )
        }

        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun startForegroundCompat(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerRunningLive.postValue(false)
        NotificationHelper.cancelForeground(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

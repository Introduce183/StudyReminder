package com.study.reminder.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.study.reminder.service.TimerService

class TimerViewModel : ViewModel() {

    val timerRunning = MutableLiveData(false)
    val remainingTime = MutableLiveData(0)

    val enableNotification = MutableLiveData(true)
    val enableDynamicIsland = MutableLiveData(false)
    val enableSmallWindow = MutableLiveData(false)
    val enableFullScreen = MutableLiveData(false)
    val enableForceHome = MutableLiveData(false)
    val enableAiScreenRead = MutableLiveData(false)
    val selectedCharacter = MutableLiveData(0)

    private var remainingObserver: Observer<Int>? = null
    private var runningObserver: Observer<Boolean>? = null

    fun startTimer(
        totalMinutes: Int,
        enableNotification: Boolean,
        enableDynamicIsland: Boolean,
        enableSmallWindow: Boolean,
        enableFullScreen: Boolean,
        enableForceHome: Boolean,
        enableAiScreen: Boolean,
        characterIndex: Int,
        context: Context
    ) {
        timerRunning.value = true
        remainingTime.value = totalMinutes * 60

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION_MINUTES, totalMinutes)
            putExtra(TimerService.EXTRA_ENABLE_NOTIFICATION, enableNotification)
            putExtra(TimerService.EXTRA_ENABLE_DYNAMIC_ISLAND, enableDynamicIsland)
            putExtra(TimerService.EXTRA_ENABLE_SMALL_WINDOW, enableSmallWindow)
            putExtra(TimerService.EXTRA_ENABLE_FULLSCREEN, enableFullScreen)
            putExtra(TimerService.EXTRA_ENABLE_FORCE_HOME, enableForceHome)
        }
        context.startForegroundService(intent)

        // Remove previous observers before registering new ones
        remainingObserver?.let { TimerService.remainingTimeLive.removeObserver(it) }
        runningObserver?.let { TimerService.timerRunningLive.removeObserver(it) }

        remainingObserver = Observer { remaining ->
            remainingTime.value = remaining
            if (remaining <= 0) timerRunning.value = false
        }
        runningObserver = Observer { running ->
            timerRunning.value = running
        }

        TimerService.remainingTimeLive.observeForever(remainingObserver!!)
        TimerService.timerRunningLive.observeForever(runningObserver!!)
    }

    fun stopTimer(context: Context) {
        timerRunning.value = false
        removeObservers()
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun removeObservers() {
        remainingObserver?.let { TimerService.remainingTimeLive.removeObserver(it) }
        runningObserver?.let { TimerService.timerRunningLive.removeObserver(it) }
        remainingObserver = null
        runningObserver = null
    }

    override fun onCleared() {
        removeObservers()
        super.onCleared()
    }

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString("api_key", key).apply()
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .getString("api_key", "") ?: ""
    }
}

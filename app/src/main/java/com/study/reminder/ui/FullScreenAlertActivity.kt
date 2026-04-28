package com.study.reminder.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.study.reminder.databinding.ActivityFullscreenAlertBinding
import com.study.reminder.helper.CharacterHelper
import com.study.reminder.helper.NotificationHelper

class FullScreenAlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenAlertBinding

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_EMOJI = "extra_emoji"
        const val EXTRA_GO_HOME_ON_SHOW = "extra_go_home_on_show"

        fun createIntent(
            context: Context,
            message: String,
            emoji: String,
            goHomeOnShow: Boolean = false
        ): Intent {
            return Intent(context, FullScreenAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_EMOJI, emoji)
                putExtra(EXTRA_GO_HOME_ON_SHOW, goHomeOnShow)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityFullscreenAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Keep the reminder visible until the user confirms or it auto-dismisses.
            }
        })

        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "该开始学习了"
        val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "!"
        val goHomeOnShow = intent.getBooleanExtra(EXTRA_GO_HOME_ON_SHOW, false)

        binding.tvAlertMessage.text = message
        binding.tvEmoji.text = emoji
        binding.ivCharacter.setImageResource(CharacterHelper.getCharacterDrawable())
        binding.tvCharacterName.text = CharacterHelper.getCharacterName()

        binding.ivCharacter.translationY = 600f
        binding.ivCharacter.animate().translationY(0f).setDuration(400).start()
        binding.tvAlertMessage.alpha = 0f
        binding.tvAlertMessage.animate().alpha(1f).setStartDelay(300).setDuration(400).start()

        binding.btnConfirm.setOnClickListener {
            NotificationHelper.cancelAll(this)
            goHome()
            finish()
        }

        val autoDismissDelay = if (goHomeOnShow) 1200L else 10_000L
        binding.root.postDelayed({
            if (!isFinishing) {
                goHome()
                finish()
            }
        }, autoDismissDelay)
    }

    private fun goHome() {
        runCatching {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}

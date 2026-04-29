package com.study.reminder.helper

import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

object ButtonMotionHelper {

    fun attachLiquidPress(vararg views: View?) {
        views.forEach { view ->
            view ?: return@forEach
            view.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> animatePressed(v, true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePressed(v, false)
                }
                false
            }
        }
    }

    private fun animatePressed(view: View, pressed: Boolean) {
        val targetScale = if (pressed) 0.965f else 1f
        val targetAlpha = if (pressed) 0.94f else 1f
        val targetTranslationY = if (pressed) 2.5f else 0f
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .alpha(targetAlpha)
            .translationY(targetTranslationY)
            .setDuration(if (pressed) 90L else 170L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}

package de.felixnuesse.disky.ui.utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView

class TextFading {

    companion object {
        fun fadeTextview(text: String, view: TextView) {
            if(view.text == text) {
                return
            }
            view.visibility = View.VISIBLE

            val fadeIn = AlphaAnimation(0.0f, 1.0f)
            val fadeOut = AlphaAnimation(1.0f, 0.0f)
            fadeIn.duration = 300
            fadeOut.duration = 300

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation) {
                    view.text = text
                    view.startAnimation(fadeIn)
                }

            })
            view.startAnimation(fadeOut)
        }
    }
}
package de.felixnuesse.disky.ui.utils

import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import androidx.core.graphics.toColorInt
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import de.felixnuesse.disky.extensions.tag

class LottieColorizer {

    companion object {
        fun colorizeLottie(lottie: LottieAnimationView, theme: Resources.Theme) {
            val primaryColor = TypedValue()
            val targetColor = Color.valueOf("#FF00FF".toColorInt())
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryDark, primaryColor, true)

            Log.e(tag(), "prepared ui...")
            lottie.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR
            ) {
                val svg = Color.valueOf(it.startValue)
                if(svg == targetColor) {
                    primaryColor.data
                } else {
                    it.startValue
                }
            }
        }
    }
}
package de.felixnuesse.disky.ui.utils

import android.view.View
import android.widget.TextView

class TextFading {

    companion object {
        fun fadeTextview(text: String, view: TextView) {

            view.visibility = View.VISIBLE
            view.text = text

            // it is likely, that the animators are broken and interfering with each other.
         // use singletons to manage them, when readding this function
        }
    }
}
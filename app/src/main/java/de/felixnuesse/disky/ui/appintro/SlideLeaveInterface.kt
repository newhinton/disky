package de.felixnuesse.disky.ui.appintro

interface SlideLeaveInterface {

    fun allowSlideLeave(id: String): Boolean

    fun onSlideLeavePrevented(id: String)
}
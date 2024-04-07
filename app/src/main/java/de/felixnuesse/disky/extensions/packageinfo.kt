package de.felixnuesse.disky.extensions

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable


fun Any.getAppname(packagename: String, context: Context): String {
    val packageManager = context.packageManager
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packagename, 0)
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch (e: NameNotFoundException) {
        packagename
    }
}


fun Any.getAppIcon(packagename: String, context: Context): Drawable? {
    val packageManager = context.packageManager
    try {

        val applicationInfo = packageManager.getApplicationInfo(packagename, 0)
        return applicationInfo.loadIcon(packageManager)
    } catch (e: NameNotFoundException) {
        return null
    }
}
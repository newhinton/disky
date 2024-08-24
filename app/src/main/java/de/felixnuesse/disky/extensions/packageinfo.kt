package de.felixnuesse.disky.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat


fun Any.getAppname(packagename: String, context: Context): String {
    val packageManager = context.packageManager
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packagename, 0)
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch (e: NameNotFoundException) {
        packagename
    }
}


fun Any.getAppIcon(packageName: String, context: Context): Drawable? {
    val packageManager = context.packageManager
    try {

        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        return applicationInfo.loadIcon(packageManager)
    } catch (e: NameNotFoundException) {
        return null
    }
}

fun Any.getAppIconDisabled(packageName: String, context: Context): Drawable? {
    val drawable = getAppIcon(packageName, context)
    val matrix = ColorMatrix()
    matrix.setSaturation(0f)
    val filter = ColorMatrixColorFilter(matrix)
    drawable?.colorFilter = filter
    return drawable
}

fun Any.startApp(packageName: String, context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { ContextCompat.startActivity(context, it, null) }
}

fun Any.startAppSettings(packageName: String, context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", packageName, null)
    intent.let { ContextCompat.startActivity(context, it, null) }
}

fun Any.isAppEnabled(packageName: String, context: Context): Boolean {
    val packageManager = context.packageManager
    val info = packageManager.getApplicationInfo(packageName,0)
    return info.enabled
}
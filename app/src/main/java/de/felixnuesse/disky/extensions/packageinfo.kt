package de.felixnuesse.disky.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
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


fun Any.getAppIcon(packagename: String, context: Context): Drawable? {
    val packageManager = context.packageManager
    try {

        val applicationInfo = packageManager.getApplicationInfo(packagename, 0)
        return applicationInfo.loadIcon(packageManager)
    } catch (e: NameNotFoundException) {
        return null
    }
}

fun Any.startApp(packagename: String, context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(packagename)
    intent?.let { ContextCompat.startActivity(context, it, null) }
}

fun Any.startAppSettings(packagename: String, context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", packagename, null)
    intent.let { ContextCompat.startActivity(context, it, null) }
}
package de.felixnuesse.disky.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat


class PermissionManager(private var mContext: Context) {

    companion object {
        private const val REQ_ALL_FILES_ACCESS = 3101

        fun getNotificationSettingsIntent(context: Context): Intent {
            return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    fun hasAllRequiredPermissions(): Boolean {
        if(!grantedStorage()) {
            return false
        }
        return true
    }

    fun hasAllPermissions(): Boolean {
        if(!grantedNotifications()) {
            return false
        }

        return hasAllRequiredPermissions()
    }

    fun grantedStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    fun grantedNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
           ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(mContext).areNotificationsEnabled()
        }
    }

    fun requestStorage(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.fromParts(
            "package",
            activity.packageName,
            null
        )
        activity.startActivityForResult(intent, REQ_ALL_FILES_ACCESS)
    }
}
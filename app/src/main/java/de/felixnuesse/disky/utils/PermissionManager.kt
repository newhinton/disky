package de.felixnuesse.disky.utils

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import de.felixnuesse.disky.extensions.tag


class PermissionManager(private var mContext: Context) {

    companion object {
        private const val REQ_ALL_FILES_ACCESS = 3101
        private const val REQ_USAGE_PERMISSION_ACCESS = 3102
        private const val REQ_NOTIFICATION_ACCESS = 3102

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
        return Environment.isExternalStorageManager()
    }

    fun grantedNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
           ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(mContext).areNotificationsEnabled()
        }
    }

    fun grantedUsageStats(): Boolean {
        var appop = mContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appop.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            mContext.packageName
        )
        return (mode == AppOpsManager.MODE_ALLOWED);
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

    fun requestUsageStats(activity: Activity) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.data = Uri.fromParts(
            "package",
            activity.packageName,
            null
        )

        activity.startActivityForResult(intent, REQ_USAGE_PERMISSION_ACCESS)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun registerInitialRequestNotificationPermission(activity: AppCompatActivity): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { granted ->
            Log.e(tag(), "granted")
        }
    }


    fun requestNotificationPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        activity.startActivityForResult(intent, REQ_USAGE_PERMISSION_ACCESS)
    }
}
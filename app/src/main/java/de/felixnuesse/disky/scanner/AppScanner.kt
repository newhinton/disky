package de.felixnuesse.disky.scanner

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageVolume
import android.util.Log
import de.felixnuesse.disky.R
import de.felixnuesse.disky.extensions.getStorageUUID
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StorageBranch
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import de.felixnuesse.disky.model.StorageLeaf
import de.felixnuesse.disky.utils.PermissionManager

class AppScanner(var mContext: Context, var callback: ScannerCallback?) {


    fun scanApps(root: StoragePrototype, selectedStorageVolume: StorageVolume) {

        val appfolder = StorageBranch(mContext.getString(R.string.apps), StorageType.APP_COLLECTION)

        if (!PermissionManager(mContext).grantedUsageStats()) {
            return
        }

        val storageStatManager = mContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        val packageManager = mContext.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val uuid = getStorageUUID(selectedStorageVolume)
        if (uuid == null) {
            Log.e(tag(), "The provided storage volume was invalid!")
            return
        }
        for (packageInfo in packages) {

            val stats = storageStatManager.queryStatsForPackage(
                uuid, packageInfo.packageName, Process.myUserHandle()
            )

            val app = StorageBranch(packageInfo.packageName, StorageType.APP)

            var appsize = 0L
            if(stats.appBytes > 0) {
                val name = mContext.getString(R.string.apppackage_size_app)
                val entry = StorageLeaf(name, StorageType.APP_APK, stats.appBytes)
                app.addChildren(entry)
                appsize+=stats.appBytes
            }

            if(stats.cacheBytes > 0) {
                val name = mContext.getString(R.string.apppackage_size_cache)
                val entry = StorageLeaf(name, StorageType.APP_CACHE, stats.cacheBytes)
                app.addChildren(entry)
                appsize+=stats.cacheBytes
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if(stats.externalCacheBytes > 0) {
                    val name = mContext.getString(R.string.apppackage_size_externalcache)
                    val entry = StorageLeaf(name, StorageType.APP_CACHE_EXTERNAL, stats.externalCacheBytes)
                    app.addChildren(entry)
                    appsize+=stats.externalCacheBytes
                }
            }

            if(stats.dataBytes > 0) {
                val name = mContext.getString(R.string.apppackage_size_data)
                val entry = StorageLeaf(name, StorageType.APP_DATA, stats.dataBytes)
                app.addChildren(entry)
                appsize+=stats.dataBytes
            }

            if(appsize>0) {
                appfolder.addChildren(app)
                callback?.foundLeaf(app.getCalculatedSize())
            }
        }
        root.addChildren(appfolder)
    }


}
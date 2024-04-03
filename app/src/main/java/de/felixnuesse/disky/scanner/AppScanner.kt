package de.felixnuesse.disky.scanner

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import androidx.appcompat.content.res.AppCompatResources
import de.felixnuesse.disky.R
import de.felixnuesse.disky.model.AppStorageElementEntry
import de.felixnuesse.disky.model.AppdataStorageElementEntry
import de.felixnuesse.disky.model.FileStorageElementEntry
import de.felixnuesse.disky.model.FolderStorageElementEntry
import de.felixnuesse.disky.model.StorageElementEntry
import de.felixnuesse.disky.model.StorageElementType
import de.felixnuesse.disky.utils.PermissionManager

class AppScanner(var mContext: Context) {


    fun scan(): StorageElementEntry {

        var root = StorageElementEntry("/")

        //https://tomas-repcik.medium.com/listing-all-installed-apps-in-android-13-via-packagemanager-3b04771dc73
        val pm = mContext.packageManager
        val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            pm.getInstalledApplications(0)
        }

        appInfos.forEach {
            println(it.name + "" + it.packageName)
            val storageStatsManager = mContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val storageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes

            it.category


            var folder = StorageElementEntry(it.name?: it.packageName)
            folder.parent=root
            root.getChildren().add(folder)


            // todo: do i have to add them up? Or filter by storage uuid?
            for (storageVolume in storageVolumes) {
                val t = storageVolume.storageUuid?.let { it1 -> storageStatsManager.queryStatsForPackage(it1, it.packageName, Process.myUserHandle()) }

                folder.addChildren(FileStorageElementEntry("appBytes", t?.appBytes?: 0))
                folder.addChildren(FileStorageElementEntry("cacheBytes", t?.cacheBytes?: 0))
                folder.addChildren(FileStorageElementEntry("externalCacheBytes", t?.externalCacheBytes?: 0))
                folder.addChildren(FileStorageElementEntry("dataBytes", t?.appBytes?: 0))
                //t?.externalCacheBytes

            }
        }
        return root
    }

    fun scanApps(root: StorageElementEntry) {

        val appfolder = FolderStorageElementEntry(mContext.getString(R.string.apps))
        appfolder.storageType = StorageElementType.SPECIAL_APPFOLDER


        if (!PermissionManager(mContext).grantedUsageStats()) {
            return
        }

        val storageStatManager = mContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        val packageManager = mContext.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {

            //Todo: Check for proper UUID
            var stats = storageStatManager.queryStatsForPackage(
                StorageManager.UUID_DEFAULT, packageInfo.packageName, Process.myUserHandle()
            )

            val app = AppStorageElementEntry(packageInfo.packageName, stats.appBytes+stats.dataBytes)
            app.overrideIcon = packageManager.getApplicationIcon(packageInfo.packageName)

            var appsize = 0L
            if(stats.appBytes > 0) {
                val name = mContext.getString(R.string.apppackage_size_app)
                val entry = AppdataStorageElementEntry(name, stats.appBytes)
                entry.overrideIcon = AppCompatResources.getDrawable(mContext, R.drawable.icon_android)
                app.addChildren(entry)
                appsize+=stats.appBytes
            }

            if(stats.cacheBytes > 0) {
                val name = mContext.getString(R.string.apppackage_size_cache)
                val entry = AppdataStorageElementEntry(name, stats.cacheBytes)
                entry.overrideIcon = AppCompatResources.getDrawable(mContext, R.drawable.icon_cache)
                app.addChildren(entry)
                appsize+=stats.cacheBytes
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if(stats.externalCacheBytes > 0) {
                    val name = mContext.getString(R.string.apppackage_size_externalcache)
                    val entry = AppdataStorageElementEntry(name, stats.externalCacheBytes)
                    entry.overrideIcon = AppCompatResources.getDrawable(mContext, R.drawable.icon_sd)
                    app.addChildren(entry)
                    appsize+=stats.externalCacheBytes
                }
            }

            if(stats.dataBytes > 0) {
                val name = mContext.getString(R.string.apppackage_size_data)
                val entry = AppdataStorageElementEntry(name, stats.dataBytes)
                entry.overrideIcon = AppCompatResources.getDrawable(mContext, R.drawable.icon_account)
                app.addChildren(entry)
                appsize+=stats.dataBytes
            }

            if(appsize>0) {
                appfolder.addChildren(app)
            }
        }
        root.addChildren(appfolder)
    }


}
package de.felixnuesse.disky.scanner

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
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
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AppScanner(var mContext: Context, var callback: ScannerCallback?) {


    val cores = Runtime.getRuntime().availableProcessors()
    val executor = Executors.newFixedThreadPool(cores) as ThreadPoolExecutor
    val storageStatManager = mContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

    val stack = ConcurrentLinkedDeque(listOf<ApplicationInfo>())
    val results = CopyOnWriteArrayList<StoragePrototype>()


    fun scanApps(root: StoragePrototype, selectedStorageVolume: StorageVolume) {
        val appfolder = StorageBranch(mContext.getString(R.string.apps), StorageType.APP_COLLECTION)

        if (!PermissionManager(mContext).grantedUsageStats()) {
            return
        }

        val packageManager = mContext.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        if(!selectedStorageVolume.isPrimary) {
            Timber.tag(tag()).e("The provided storage volume is not a valid scan target for apps!")
        }

        stack.addAll(packages)

        repeat(cores) {
            executor.submit {
                while (true) {
                    val app = stack.pollFirst()?: break
                    process(app, StorageManager.UUID_DEFAULT)
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        results.forEach {
            appfolder.addChildren(it)
        }
        root.addChildren(appfolder)
    }


    private fun process(packageInfo: ApplicationInfo, uuid: UUID) {
        val app = StorageBranch(packageInfo.packageName, StorageType.APP)
        val stats = storageStatManager.queryStatsForPackage(
            uuid, packageInfo.packageName, Process.myUserHandle()
        )


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
            val entry = StorageLeaf(name, StorageType.APP_DATA, stats.dataBytes - stats.cacheBytes)
            app.addChildren(entry)
            appsize+=stats.dataBytes - stats.cacheBytes
        }

        if(appsize>0) {
            results.add(app)
            callback?.foundLeaf(app.getCalculatedSize())
        }
    }

}

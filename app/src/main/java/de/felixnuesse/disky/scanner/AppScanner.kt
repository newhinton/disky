package de.felixnuesse.disky.scanner

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import de.felixnuesse.disky.model.FileEntry
import de.felixnuesse.disky.model.FolderEntry

class AppScanner(var mContext: Context) {


    fun scan(): FolderEntry {

        var root = FolderEntry("/")

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


            var folder = FolderEntry(it.name?: it.packageName)
            folder.parent=root
            root.children.add(folder)


            // todo: do i have to add them up? Or filter by storage uuid?
            for (storageVolume in storageVolumes) {

                val t = storageVolume.storageUuid?.let { it1 -> storageStatsManager.queryStatsForPackage(it1, it.packageName, Process.myUserHandle()) }

                folder.children.add(FileEntry("appBytes", t?.appBytes?: 0))
                folder.children.add(FileEntry("cacheBytes", t?.cacheBytes?: 0))
                folder.children.add(FileEntry("externalCacheBytes", t?.externalCacheBytes?: 0))
                folder.children.add(FileEntry("dataBytes", t?.appBytes?: 0))
                //t?.externalCacheBytes

            }


        }

        return root


    }


}
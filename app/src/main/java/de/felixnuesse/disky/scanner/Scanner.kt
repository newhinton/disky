package de.felixnuesse.disky.scanner

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.content.Context.STORAGE_STATS_SERVICE
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import de.felixnuesse.disky.R
import de.felixnuesse.disky.extensions.getFreeSpace
import de.felixnuesse.disky.extensions.getTotalSpace
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageResult
import timber.log.Timber

class Scanner(private var mContext: Context, private var callback: ScannerCallback?) {

    private var storageStatsManager = mContext.getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager
    private var storageManager = mContext.getSystemService(STORAGE_SERVICE) as StorageManager
    
    private var fsScanner: ScannerInterface? = null
    private var stopped = false
    private var lastTimestamp = System.currentTimeMillis()

    fun printRuntimeTime(additionalLabel: String = "") {
        val took = System.currentTimeMillis() - lastTimestamp
        Timber.tag(tag()).e("Scannertime: $took ms (label='$additionalLabel')")
        lastTimestamp = System.currentTimeMillis()
    }

    fun stop() {
        fsScanner?.stop()
    }

    fun scan(storage: String, subpath: String?): StorageResult? {
        lastTimestamp = System.currentTimeMillis()
        val selectedStorage = findStorageByNameOrUUID(storage)
        val rootElement: StoragePrototype?

        // todo: If we re-scan a sub-path, this is wrong. We should use the parent-folder-size, but
        //      this is hard to do since we don't know the size.
        callback?.setMaxSize(getTotalSpace(storageStatsManager, selectedStorage))
        val subfolder = subpath?.replace(selectedStorage.directory!!.absolutePath+"/", "")?: ""

        fsScanner = FullyMulticoreFsScanner(callback) // FsScanner(callback)
        if(selectedStorage.directory == null) {
            Timber.tag(tag()).e("There was an error loading data!")
            return null
        }
        printRuntimeTime("startup and prep")

        rootElement = fsScanner?.scan(selectedStorage.directory!!, subfolder)
        if(stopped) {
            return null
        }
        printRuntimeTime("fs scan")

        val isAppFolderUpdate = subfolder.isBlank() || subfolder == mContext.getString(R.string.apps)
        // Don't scan for system and apps on external sd card
        if(selectedStorage.isPrimary && rootElement != null) {
            if(isAppFolderUpdate) {

                val nowMulti = System.currentTimeMillis()
                AppScanner(mContext, callback).scanApps(rootElement, selectedStorage)
                Timber.tag(tag())
                    .e("Time: ${System.currentTimeMillis() - nowMulti} ms (AppScanner)")

                if(stopped) {
                    return null
                }
            }
            printRuntimeTime("app scan")
            if (subfolder.isBlank()) {
                SystemScanner(mContext, callback)
                    .scanApps(
                        rootElement,
                        getTotalSpace(storageStatsManager, selectedStorage),
                        getFreeSpace(storageStatsManager, selectedStorage)
                    )
                if(stopped) {
                    return null
                }
            }
            printRuntimeTime("sys scan")
        }

        val result = StorageResult()
        result.scannedVolume = selectedStorage
        result.rootElement = rootElement
        result.free = getFreeSpace(storageStatsManager, selectedStorage)
        result.used = rootElement?.getCalculatedSize()?: 0
        result.total = getTotalSpace(storageStatsManager, selectedStorage)
        result.isPartialScan = subfolder.isNotBlank()
        printRuntimeTime("res prep")
        return result
    }


    private fun findStorageByNameOrUUID(name: String): StorageVolume {
        storageManager.storageVolumes.forEach {
            if((name == it.getDescription(mContext)) or (name == it.uuid.toString())){
                return it
            }
        }

        val msg = "We could not determine the used storage with name: $name. This is a fatal error!"
        Timber.tag(tag()).e(msg)
        throw RuntimeException(msg)
    }

}
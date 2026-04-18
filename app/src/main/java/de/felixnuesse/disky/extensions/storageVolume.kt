package de.felixnuesse.disky.extensions

import android.app.usage.StorageStatsManager
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import timber.log.Timber
import java.util.UUID

fun Any.getTotalSpace(storageStatsManager: StorageStatsManager, selectedStorage: StorageVolume): Long {
    return if(selectedStorage.isPrimary) {
        storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
    } else {
        selectedStorage.directory?.totalSpace ?: -1
    }
}

fun Any.getFreeSpace(storageStatsManager: StorageStatsManager, selectedStorage: StorageVolume): Long {
    return if(selectedStorage.isPrimary) {
        storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
    } else {

        selectedStorage.directory?.freeSpace ?: -1
    }
}
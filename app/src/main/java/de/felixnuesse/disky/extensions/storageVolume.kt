package de.felixnuesse.disky.extensions

import android.app.usage.StorageStatsManager
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import timber.log.Timber
import java.util.UUID


fun Any.getStorageUUID(selectedStorage: StorageVolume): UUID? {
    var uuid: UUID? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if(selectedStorage.storageUuid != null) {
            uuid = selectedStorage.storageUuid
        } else {
            Timber.tag(tag()).e("WARNING: Cant get Storage UUID for non-permanent storage.")
        }
    } else {
        Timber.tag(tag()).e("WARNING: Cant get Storage UUID for non-permanent storage.")
        uuid = UUID.nameUUIDFromBytes(selectedStorage.uuid?.toByteArray())
    }

    return uuid
}

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
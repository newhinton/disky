package de.felixnuesse.disky.extensions

import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import java.util.UUID


fun Any.getStorageUUID(selectedStorage: StorageVolume): UUID? {
    var uuid: UUID? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if(selectedStorage.storageUuid != null) {
            uuid = selectedStorage.storageUuid
        } else {
            Log.e(tag(), "WARNING: Cant get Storage UUID for non-permanent storage.")
            uuid = UUID.nameUUIDFromBytes(selectedStorage.uuid?.toByteArray())
        }
    } else {
        Log.e(tag(), "WARNING: Cant get Storage UUID for non-permanent storage.")
        //uuid = UUID.nameUUIDFromBytes(selectedStorage.uuid?.toByteArray())
        val storageUuid = selectedStorage.uuid
        uuid = if(storageUuid != null) {
            UUID.nameUUIDFromBytes(selectedStorage.uuid?.let { it.toByteArray()})
        } else {
            StorageManager.UUID_DEFAULT
        }
    }

    return uuid
}
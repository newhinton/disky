package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class AppdataStorageElementEntry(var appdataName: String, private var datasize: Long): AppStorageElementEntry(appdataName, datasize) {
    init {
        storageType = StorageElementType.APPDATA
    }

    override fun getCalculatedSize(): Long {
        return datasize
    }
}
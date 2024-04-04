package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class OSStorageElementEntry(private var size: Long): StorageElementEntry("System") {
    init {
        storageType = StorageElementType.SPECIAL_SYSTEM
    }

    override fun getCalculatedSize(): Long {
        return size
    }
}
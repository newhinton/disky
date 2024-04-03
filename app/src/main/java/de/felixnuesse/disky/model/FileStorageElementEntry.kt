package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class FileStorageElementEntry(var filename: String, private var size: Long): StorageElementEntry(filename) {

    init {
        storageType = StorageElementType.FILE
    }

    override fun getCalculatedSize(): Long {
        return size
    }
}
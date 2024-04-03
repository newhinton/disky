package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class FolderStorageElementEntry(private var foldername: String): StorageElementEntry(foldername) {
    init {
        storageType = StorageElementType.FOLDER
    }
}
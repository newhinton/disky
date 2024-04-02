package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class FileEntry(var filename: String, private var size: Long): FolderEntry(filename) {

    override fun getCalculatedSize(): Long {
        return size
    }
}
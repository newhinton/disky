package de.felixnuesse.disky.model

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class AppStorageElementEntry(var filename: String, private var size: Long): StorageElementEntry(filename) {
    init {
        storageType = StorageElementType.APP
    }

    @Transient var overrideIcon: Drawable? = null

    override fun getCalculatedSize(): Long {
        return size
    }
}
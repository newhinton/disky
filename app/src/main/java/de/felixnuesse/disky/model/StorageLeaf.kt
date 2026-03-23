package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class StorageLeaf(
    var leafname: String,
    var leafStorageType: StorageType = StorageType.GENERIC,
    var size: Long = 0
): StoragePrototype(leafname, leafStorageType) {

    override fun getCalculatedSize(forceRecalculation: Boolean): Long {
        return size
    }
}
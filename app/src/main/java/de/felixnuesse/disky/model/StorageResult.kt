package de.felixnuesse.disky.model

import android.os.storage.StorageVolume
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class StorageResult {

    @Transient var scannedVolume: StorageVolume? = null
    var rootElement: StoragePrototype? = null
    var isPartialScan = false

    var free = 0L
    var used = 0L
    var total = 0L

    fun asJsonString(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromString(json: String): StorageResult {
            val result:StorageResult = Json.decodeFromString(json)
            // now restore parent-child relationships
            result.rootElement?.fixChildren()
            return result
        }
    }
}
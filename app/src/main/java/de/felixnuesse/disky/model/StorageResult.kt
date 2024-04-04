package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class StorageResult {

    var rootElement: StoragePrototype? = null

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
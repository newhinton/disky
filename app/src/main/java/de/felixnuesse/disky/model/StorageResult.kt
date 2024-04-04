package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
class StorageResult {

    var rootElement: StorageElementEntry? = null

    var free = 0L
    var used = 0L
    var total = 0L


    fun asJSON(): JSONObject {
        return JSONObject(Json.encodeToString(this))
    }

    companion object {
        fun fromString(json: String): StorageResult {
            return Json.decodeFromString(json)
        }
    }
}
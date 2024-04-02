package de.felixnuesse.disky.model


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
open class FolderEntry(var name: String) {

    var scanDate = System.currentTimeMillis()
    @Transient var parent: FolderEntry? = null
    var children: ArrayList<FolderEntry> = arrayListOf()
    var percent: Int = 0


    @Transient var calculatedSize: Long? = null

    open fun getCalculatedSize(): Long {
        if(calculatedSize == null) {
            calculatedSize = 0
            children.forEach {
                calculatedSize = calculatedSize!! + it.getCalculatedSize()
            }
        }

        return calculatedSize!!
    }

    fun getParentPath(): String {

        if(parent != null) {
            return parent!!.getParentPath()+"/"+name
        }

        return name
    }

    fun asJSON(): JSONObject {
        return JSONObject(Json.encodeToString(this))
    }
}
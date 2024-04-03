package de.felixnuesse.disky.model


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
open class StorageElementEntry(var name: String) {

    var scanDate = System.currentTimeMillis()
    @Transient var parent: StorageElementEntry? = null
    private var children: ArrayList<StorageElementEntry> = arrayListOf()
    var storageType = StorageElementType.GENERIC
    @Transient var calculatedSize: Long? = null



    //Todo: What is percent????
    var percent: Int = 0


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

    fun getChildren(): ArrayList<StorageElementEntry> {
        return children
    }

    fun clearChildren() {
        children = arrayListOf()
    }

    fun addChildren(vararg child: StorageElementEntry) {
        child.forEach {
            it.parent=this
            children.add(it)
        }
    }


    fun asJSON(): JSONObject {
        return JSONObject(Json.encodeToString(this))
    }
}
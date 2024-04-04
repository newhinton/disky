package de.felixnuesse.disky.model


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class StoragePrototype(var name: String, var storageType: StorageType = StorageType.GENERIC) {

    var scanDate = System.currentTimeMillis()
    @Transient var parent: StoragePrototype? = null
    private var children: ArrayList<StoragePrototype> = arrayListOf()

    @Transient var calculatedSize: Long? = null

    //Todo: What is percent????
    var percent: Int = 0


    open fun getCalculatedSize(): Long {
        calculatedSize = 0
        children.forEach {
            calculatedSize = calculatedSize!! + it.getCalculatedSize()
        }
        return calculatedSize!!
    }

    fun getParentPath(): String {
        if(parent != null) {
            return parent!!.getParentPath()+"/"+name
        }
        return name
    }

    fun getChildren(): ArrayList<StoragePrototype> {
        return children
    }

    fun clearChildren() {
        children = arrayListOf()
    }

    fun addChildren(vararg child: StoragePrototype) {
        child.forEach {
            it.parent=this
            children.add(it)
        }
    }

    fun fixChildren() {
        children.forEach {
            it.fixChildren()
            it.parent=this
        }
    }

    fun asJsonString(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromString(json: String): StorageResult {
            return Json.decodeFromString(json)
        }

    }
}
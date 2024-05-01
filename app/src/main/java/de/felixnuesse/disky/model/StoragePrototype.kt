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
            var path = parent!!.getParentPath()
            path = if(path.endsWith("/")) {
                path + name
            } else {
                path + "/" + name
            }
            return path
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
    fun addChildren(newchildren: ArrayList<StoragePrototype>) {
        newchildren.forEach {
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

    /**
     * This merges a partial storage tree into this one.
     * Its a breath-first approach.
     *
     * For generic items, children are replaced for the found root-partial-tree.
     * For other items, special handling is applied. (Apps are replaced in-place)
     *
     * @return Boolean True, if it found a place to merge
     */
    fun mergePartialTree(partialTree: StoragePrototype): Boolean{
        children.forEach {
            if(it.getParentPath() == partialTree.getParentPath()) {
                it.clearChildren()

                when(it.storageType) {
                    StorageType.APP_COLLECTION -> {
                        // it will always contain the "app"-folder. Remove it, and add children
                        it.addChildren(partialTree.getChildren()[0].getChildren())
                    }
                    else -> {
                        it.addChildren(partialTree.getChildren())
                    }
                }
                return true
            }
        }
        children.forEach {
            if(it.mergePartialTree(partialTree)){
                return true
            }
        }
        return false
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
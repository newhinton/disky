package de.felixnuesse.disky.model

open class FileSystemStructure(var name: String) {

    var scanDate = System.currentTimeMillis()
    var parent: FileSystemStructure? = null
    var children: ArrayList<FileSystemStructure> = arrayListOf()


    open fun getCalculatedSize(): Long {
        var size = 0L
        children.forEach {
            size += it.getCalculatedSize()
        }
        return size
    }

    fun getParentPath(): String {

        if(parent != null) {
            return parent!!.getParentPath()+"/"+name
        }

        return name
    }
}
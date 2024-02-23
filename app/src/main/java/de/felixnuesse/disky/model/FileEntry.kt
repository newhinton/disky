package de.felixnuesse.disky.model

class FileEntry(name: String, private var size: Long): FileSystemStructure(name) {

    override fun getCalculatedSize(): Long {
        return size
    }
}
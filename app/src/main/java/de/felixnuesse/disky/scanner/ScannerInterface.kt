package de.felixnuesse.disky.scanner

import de.felixnuesse.disky.model.StoragePrototype
import java.io.File

interface ScannerInterface {
    fun scan(file: File, subfolder: String): StoragePrototype

    fun stop()
}
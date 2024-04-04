package de.felixnuesse.disky.scanner

import de.felixnuesse.disky.model.StorageResult

interface ScanCompleteCallback {
    fun scanComplete(result: StorageResult)
}
package de.felixnuesse.disky.scanner

interface ScannerCallback {
    fun currentlyScanning(item: String)

    fun foundLeaf(size: Long)
}
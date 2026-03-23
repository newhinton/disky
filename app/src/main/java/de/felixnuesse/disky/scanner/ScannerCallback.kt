package de.felixnuesse.disky.scanner

interface ScannerCallback {


    fun setMaxSize(totalSpace: Long)

    fun currentlyScanning(item: String)

    fun foundLeaf(size: Long)
}
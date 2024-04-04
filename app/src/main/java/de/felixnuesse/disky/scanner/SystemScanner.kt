package de.felixnuesse.disky.scanner

import de.felixnuesse.disky.model.OSStorageElementEntry
import de.felixnuesse.disky.model.StorageElementEntry

class SystemScanner {

    fun scanApps(root: StorageElementEntry, totalSpace: Long, freeSpace: Long) {
        val sysUsage = totalSpace - freeSpace - root.getCalculatedSize()
        val sys = OSStorageElementEntry(sysUsage)
        root.addChildren(sys)
    }
}
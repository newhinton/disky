package de.felixnuesse.disky.scanner

import android.content.Context
import de.felixnuesse.disky.R
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import de.felixnuesse.disky.model.StorageLeaf

class SystemScanner(var mContext: Context, var callback: ScannerCallback?) {

    fun scanApps(root: StoragePrototype, totalSpace: Long, freeSpace: Long) {
        val sysUsage = totalSpace - freeSpace - root.getCalculatedSize()
        val sys = StorageLeaf(mContext.getString(R.string.system), StorageType.OS)
        sys.size = sysUsage
        callback?.foundLeaf(sys.getCalculatedSize())
        root.addChildren(sys)
    }
}
package de.felixnuesse.disky.scanner

import android.content.Context
import de.felixnuesse.disky.model.FileStorageElementEntry
import de.felixnuesse.disky.model.FolderStorageElementEntry
import de.felixnuesse.disky.model.StorageElementEntry
import java.io.File


class FsScanner(var mContext: Context, var callback: ScannerCallback?) {



    fun scan(file: File): StorageElementEntry {
        val root = StorageElementEntry(file.absolutePath+"/")
        scanFolder(root)
        return root
    }

    private fun scanFolder(folder: StorageElementEntry): StorageElementEntry {
        val directory = File(folder.getParentPath())
        callback?.currentlyScanning(directory.absolutePath)
        directory.listFiles()?.forEach {
            if(it.isFile){
                var fe = FileStorageElementEntry(it.name, it.length())
                fe.parent=folder
                folder.addChildren(fe)
            }
            if(it.isDirectory){
                var storageElementEntry = FolderStorageElementEntry(it.name)
                storageElementEntry.parent=folder
                folder.addChildren(scanFolder(storageElementEntry))
            }
        }
        return folder
    }
}
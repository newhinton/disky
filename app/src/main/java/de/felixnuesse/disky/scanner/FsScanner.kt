package de.felixnuesse.disky.scanner

import android.content.Context
import de.felixnuesse.disky.model.StorageBranch
import de.felixnuesse.disky.model.StorageLeaf
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import java.io.File


class FsScanner(var mContext: Context, var callback: ScannerCallback?) {



    fun scan(file: File): StoragePrototype {
        val root = StorageBranch(file.absolutePath+"/")
        scanFolder(root)
        return root
    }

    private fun scanFolder(folder: StoragePrototype): StoragePrototype {
        val directory = File(folder.getParentPath())
        callback?.currentlyScanning(directory.absolutePath)
        directory.listFiles()?.forEach {
            if(it.isFile){
                val fe = StorageLeaf(it.name, StorageType.FILE, it.length())
                fe.parent=folder
                folder.addChildren(fe)
            }
            if(it.isDirectory){
                val storageElementEntry = StorageBranch(it.name, StorageType.FOLDER)
                storageElementEntry.parent=folder
                folder.addChildren(scanFolder(storageElementEntry))
            }
        }
        return folder
    }
}
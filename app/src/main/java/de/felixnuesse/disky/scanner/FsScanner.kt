package de.felixnuesse.disky.scanner

import android.util.Log
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StorageBranch
import de.felixnuesse.disky.model.StorageLeaf
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import java.io.File


class FsScanner(var callback: ScannerCallback?) {

    var stopped = false

    fun scan(file: File, subfolder: String): StoragePrototype {
        val rootfolder = if(file.absolutePath.endsWith("/")) {
            file.absolutePath + subfolder
        } else {
            file.absolutePath + "/" + subfolder
        }
        val root = StorageBranch(rootfolder)
        scanFolder(root)
        return root
    }

    private fun scanFolder(folder: StoragePrototype): StoragePrototype {
        val directory = File(folder.getParentPath())
        callback?.currentlyScanning(directory.absolutePath)
        directory.listFiles()?.forEach {
            if(stopped){
                return folder
            }
            if(it.isFile){
                val fe = StorageLeaf(it.name, StorageType.FILE, it.length())
                fe.parent=folder
                folder.addChildren(fe)
                callback?.foundLeaf(fe.getCalculatedSize())
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
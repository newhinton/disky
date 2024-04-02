package de.felixnuesse.disky.scanner

import android.content.Context
import android.os.Environment
import de.felixnuesse.disky.model.FileEntry
import de.felixnuesse.disky.model.FolderEntry
import java.io.File


class FsScanner(var mContext: Context, var callback: ScannerCallback?) {



    fun scan(): FolderEntry {
        val root = FolderEntry(Environment.getExternalStorageDirectory().toString())
        scanFolder(root)
        return root
    }


    private fun scanFolder(folder: FolderEntry): FolderEntry {
        val directory = File(folder.getParentPath())
        callback?.currentlyScanning(directory.absolutePath)
        directory.listFiles()?.forEach {
            if(it.isFile){
                var fe = FileEntry(it.name, it.length())
                fe.parent=folder
                folder.children.add(fe)
            }
            if(it.isDirectory){
                var folderEntry = FolderEntry(it.name)
                folderEntry.parent=folder
                folder.children.add(scanFolder(folderEntry))
            }
        }
        return folder
    }


}
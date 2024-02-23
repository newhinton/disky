package de.felixnuesse.disky.scanner

import android.content.Context
import android.os.Environment
import android.util.Log
import de.felixnuesse.disky.model.FileEntry
import de.felixnuesse.disky.model.FileSystemStructure
import java.io.File


class FsScanner(var mContext: Context) {


    fun scan(): FileSystemStructure {

        val root = FileSystemStructure(Environment.getExternalStorageDirectory().toString())

        scanFolder(root)

        return root


    }


    private fun scanFolder(folder: FileSystemStructure): FileSystemStructure {

        val directory = File(folder.getParentPath())
        directory.listFiles()?.forEach {
            if(it.isFile){
                var fe = FileEntry(it.name, it.length())
                fe.parent=folder
                folder.children.add(fe)
            }
            if(it.isDirectory){
                var folderEntry = FileSystemStructure(it.name)
                folderEntry.parent=folder
                folder.children.add(scanFolder(folderEntry))

            }
        }
        return folder
    }


}
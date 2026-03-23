package de.felixnuesse.disky.scanner

import android.net.Uri
import android.util.Log
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StorageBranch
import de.felixnuesse.disky.model.StorageLeaf
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class FsScanner(var callback: ScannerCallback?): ScannerInterface {

    var cores = Runtime.getRuntime().availableProcessors()
    var stopped = false

    var lastScan = 0L

    val executor = Executors.newWorkStealingPool(cores) as ExecutorService

    fun submit(task: StoragePrototype) {
        executor.submit {
            scanFolder(task)
        }
    }

    override fun scan(file: File, subfolder: String): StoragePrototype {
        val start = System.currentTimeMillis()
        val result = internalSemiMultithreadedScan(file, subfolder)
        lastScan = System.currentTimeMillis()-start
        Log.e(tag(), "Time: $lastScan ms (Semi Multi-Core;$cores)")
        return result
    }

    override fun stop() {
        stopped = true
    }

    private fun getFullPath(file: File, subfolder: String): String {
        return if(file.absolutePath.endsWith("/")) {
            file.absolutePath + subfolder
        } else {
            file.absolutePath + "/" + subfolder
        }
    }

    private fun internalSemiMultithreadedScan(file: File, subfolder: String): StoragePrototype {
        val rootFolder = getFullPath(file, subfolder)
        val root = StorageBranch(rootFolder)

        submit(root)
        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
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
                fe.uri = Uri.fromFile(it).toString()
                fe.parent=folder
                folder.addChildren(fe)
                callback?.foundLeaf(fe.getCalculatedSize())
            }
            if(it.isDirectory){
                val storageElementEntry = StorageBranch(it.name, StorageType.FOLDER)
                storageElementEntry.parent=folder
                folder.addChildren(storageElementEntry)
                submit(storageElementEntry)
            }
        }
        return folder
    }
}
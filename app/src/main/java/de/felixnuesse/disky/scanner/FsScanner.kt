package de.felixnuesse.disky.scanner

import android.net.Uri
import android.util.Log
import de.felixnuesse.disky.model.StorageBranch
import de.felixnuesse.disky.model.StorageLeaf
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import java.io.File
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class FsScanner(var callback: ScannerCallback?) {

    var TAG = "FsScanner"
    var cores = Runtime.getRuntime().availableProcessors()
    var stopped = false

    var lastScan = 0L


    // newCachedThreadPool
    val executor = Executors.newWorkStealingPool(cores) as ExecutorService

    fun submit(task: StoragePrototype) {
        executor.submit {
            scanFolder(task)
        }
    }

    fun scan(file: File, subfolder: String): StoragePrototype {


        // Log.e(TAG, "file: ${file.toString()}")
        // Log.e(TAG, "subfolder: ${subfolder}")


        val nowMulti = System.currentTimeMillis()
        val rm = im_scan(file, subfolder)
        Log.e(TAG, "Time: ${System.currentTimeMillis()-nowMulti} ms (MULTI)")
        lastScan = System.currentTimeMillis()-nowMulti

        // single core: 10-11seconds
        //val nowSingle = System.currentTimeMillis()
        //val r = i_scan(file, subfolder)
        //Log.e(TAG, "Time: ${System.currentTimeMillis()-nowSingle} ms")

        return rm
    }

    private fun getFullPath(file: File, subfolder: String): String {
        return if(file.absolutePath.endsWith("/")) {
            file.absolutePath + subfolder
        } else {
            file.absolutePath + "/" + subfolder
        }
    }


    private fun im_scan(file: File, subfolder: String): StoragePrototype {
        val rootfolder = getFullPath(file, subfolder)
        val root = StorageBranch(rootfolder)

        // Log.e(TAG, "Cores: $cores")
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
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
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class FsScanner(var callback: ScannerCallback?) {

    var stopped = false


    // todo: Calculate top level files
    val stack = ConcurrentLinkedDeque(listOf<String>())
    val results = CopyOnWriteArrayList<StoragePrototype>()


    fun scan(file: File, subfolder: String): StoragePrototype {


        Log.e("TIME", "file: ${file.toString()}")
        Log.e("TIME", "subfolder: ${subfolder}")


        val nowMulti = System.currentTimeMillis()
        val rm = im_scan(file, subfolder)
        Log.e("TIME", "Time: ${System.currentTimeMillis()-nowMulti} ms (MULTI)")


        // single core: 10-11seconds
        //val nowSingle = System.currentTimeMillis()
        //val r = i_scan(file, subfolder)
        //Log.e("TIME", "Time: ${System.currentTimeMillis()-nowSingle} ms")

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

        val processors = Runtime.getRuntime().availableProcessors()
        Log.e("TIME", "Cores: $processors")

        val rootfolder = getFullPath(file, subfolder)
        val root = StorageBranch(rootfolder)

        val tlf = File(rootfolder).listFiles().map { it.name }.toList()
        Log.e("TIME", "Workable tasks: ${tlf.joinToString(", ")}")

        stack.addAll(tlf)
        val executor = Executors.newFixedThreadPool(processors) as ThreadPoolExecutor
        repeat(processors) {
            executor.submit {
                while (true) {
                    val nowMulti = System.currentTimeMillis()
                    val folderpath = stack.pollFirst()?: break
                    val flFolder = getFullPath(File(rootfolder), subfolder)
                    val res = i_scan(File(flFolder), folderpath)
                    results.add(res)
                    Log.e("TIME", "Time: ${System.currentTimeMillis()-nowMulti} ms (TASK: $folderpath)")
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)

        for (i in results) {
            root.addChildren(i)
        }

        return root
    }

    private fun i_scan(file: File, subfolder: String): StoragePrototype {
        val rootfolder = getFullPath(file, subfolder)
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
                fe.uri = Uri.fromFile(it).toString()
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
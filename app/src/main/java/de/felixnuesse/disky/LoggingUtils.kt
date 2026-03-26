package de.felixnuesse.disky

import android.content.Context
import android.os.Environment
import android.util.Log
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber
import timber.log.Timber.Tree


class LoggingUtils {

    fun configure(context: Context) {

        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val externalFilesDir = context.getExternalFilesDir(null)

        val tree = FileLoggerTree.Builder()
            .withFileName("disky-log-%g.txt")
            .withDirName((externalFilesDir ?: downloadsFolder).absolutePath)
            .withSizeLimit(5*1048576)
            .withFileLimit(20)
            .withMinPriority(Log.DEBUG)
            .appendToFile(true)
            .build()
        Timber.plant(tree)
        Timber.plant(Timber.DebugTree())

    }
}
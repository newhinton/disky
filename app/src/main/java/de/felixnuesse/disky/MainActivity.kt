package de.felixnuesse.disky

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.felixnuesse.disky.model.FileSystemStructure
import de.felixnuesse.disky.scanner.FsScanner
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var scanner = FsScanner(applicationContext)

        var root = scanner.scan()
        printDepthFirst("", root)

    }

    fun printDepthFirst(path: String, folderEntry: FileSystemStructure) {

        if(folderEntry.children.size != 0) {
            folderEntry.children.forEach {
                printDepthFirst(path+"/"+it.name, it)
            }
        } else {
            println(path+" - "+readableFileSize(folderEntry.getCalculatedSize()))
        }
    }

    //https://stackoverflow.com/a/5599842
    fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
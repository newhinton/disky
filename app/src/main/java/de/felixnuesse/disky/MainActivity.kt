package de.felixnuesse.disky

import android.R.attr.tag
import android.animation.ObjectAnimator
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import de.felixnuesse.disky.databinding.ActivityMainBinding
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.FolderEntry
import de.felixnuesse.disky.scanner.FsScanner
import de.felixnuesse.disky.scanner.ScannerCallback
import de.felixnuesse.disky.ui.ChangeFolderCallback
import de.felixnuesse.disky.ui.RecyclerViewAdapter
import de.felixnuesse.disky.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


class MainActivity : AppCompatActivity(), ScannerCallback, ChangeFolderCallback {

    private lateinit var binding: ActivityMainBinding
    private var permissions = PermissionManager(this)

    private var rootElement: FolderEntry? = null
    private var currentElement: FolderEntry? = null

    private lateinit var storageManager: StorageManager
    private lateinit var storageStatsManager: StorageStatsManager

    private var selectedStorage: StorageVolume? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!permissions.grantedStorage()) {
            permissions.requestStorage(this)
        }

        storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        storageStatsManager = getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dropdown = (binding.dropdown as MaterialAutoCompleteTextView)
        var storageList = arrayListOf<String>()
        storageManager.storageVolumes.forEach {
            storageList.add(it.mediaStoreVolumeName?: it.uuid.toString())
            if(it.isPrimary) {
                selectedStorage = it
            }
        }
        dropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, storageList))
        dropdown.setText(selectedStorage?.mediaStoreVolumeName?: selectedStorage?.uuid.toString(), false)

        binding.dropdown.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            selectedStorage = findStorageByNameOrUUID(storageList[position])
            CoroutineScope(Dispatchers.IO).launch{
                updateData()
            }
        }

        CoroutineScope(Dispatchers.IO).launch{
            updateData()
        }
    }

    fun findStorageByNameOrUUID(name: String): StorageVolume? {
        storageManager.storageVolumes.forEach {
            if((name == it.mediaStoreVolumeName) or (name == it.uuid.toString())){
                return it
            }
        }
        return null
    }

    fun updateData() {
        val scanner = FsScanner(applicationContext, this)
        if(selectedStorage?.directory == null) {
            Log.e(tag(), "There was an error loading data!")
            return
        }
        rootElement = scanner.scan(selectedStorage!!.directory!!)
        if(rootElement != null) {
            runOnUiThread {
                showFolder(rootElement!!)
            }
        }
    }

    override fun onBackPressed() {
        if (currentElement != rootElement) {
            currentElement?.parent?.let { showFolder(it) }
        } else {
            super.onBackPressed()
        }
    }

    //https://gist.github.com/li-jkwok/e460a042326e8509ada9ec23ae677bdf
    fun getTotalDiskSpace(): Long {

        val storageVolumes = storageManager.storageVolumes
        var totalBytes = 0L
        for (volume in storageVolumes) {
            val uuid = volume.uuid?.let { UUID.fromString(it) } ?: StorageManager.UUID_DEFAULT
            Log.e(tag(), "storage: $uuid")
            totalBytes += storageStatsManager.getTotalBytes(uuid)
        }
        return totalBytes
    }

    fun showFolder(currentRoot: FolderEntry) {

        currentElement = currentRoot

        val prog = currentRoot.getCalculatedSize().div(getTotalDiskSpace().toDouble())

        binding.textView2.text = readableFileSize(currentRoot.getCalculatedSize())

        ObjectAnimator
            .ofInt(binding.dataUsage, "progress", (prog*100).toInt())
            .setDuration(300)
            .start()

        //first, calculate percentages.
        var max = currentRoot.getCalculatedSize()
        currentRoot.children.forEach {
            val percentage = (it.getCalculatedSize().toFloat()/max.toFloat())
            it.percent = (percentage*100).toInt()
        }

        //second, sort children
        val l = currentElement!!.children.sortedWith(compareBy{ list -> list.getCalculatedSize()})
        currentElement!!.children = arrayListOf()
        currentElement!!.children.addAll(l.reversed())


        val recyclerView = binding.folders

        val animation: LayoutAnimationController = AnimationUtils.loadLayoutAnimation(this, R.anim.recyclerview_animation)
        recyclerView.setLayoutAnimation(animation)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        val recyclerViewAdapter = RecyclerViewAdapter(currentRoot.children, this)
        recyclerView.setAdapter(recyclerViewAdapter)
    }


    fun printDepthFirst(path: String, folderEntry: FolderEntry) {

        if(folderEntry.children.size != 0) {
            folderEntry.children.forEach {
                printDepthFirst(path+"/"+it.name, it)
            }
        } else {
            Log.e(tag(), path+" - "+ readableFileSize(folderEntry.getCalculatedSize()))
        }
    }

    override fun currentlyScanning(item: String) {
       // Log.e(tag(), "Current scan: $item")
    }

    override fun changeFolder(folder: FolderEntry) {
        showFolder(folder)
    }
}
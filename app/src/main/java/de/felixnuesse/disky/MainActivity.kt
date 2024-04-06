package de.felixnuesse.disky

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import de.felixnuesse.disky.IntroActivity.Companion.INTRO_PREFERENCES
import de.felixnuesse.disky.IntroActivity.Companion.intro_v1_0_0_completed
import de.felixnuesse.disky.background.ScanService
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_COMPLETE
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_STORAGE
import de.felixnuesse.disky.databinding.ActivityMainBinding
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageResult
import de.felixnuesse.disky.scanner.ScanCompleteCallback
import de.felixnuesse.disky.ui.ChangeFolderCallback
import de.felixnuesse.disky.ui.RecyclerViewAdapter
import de.felixnuesse.disky.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), ChangeFolderCallback, ScanCompleteCallback {

    private lateinit var binding: ActivityMainBinding

    private var rootElement: StoragePrototype? = null
    private var currentElement: StoragePrototype? = null

    private lateinit var storageManager: StorageManager
    private var selectedStorage = ""

    private var lastScanStarted = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = applicationContext.getSharedPreferences(INTRO_PREFERENCES, Context.MODE_PRIVATE)
        if (!sharedPref.getBoolean(intro_v1_0_0_completed, false)) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }

        if(!PermissionManager(this).hasAllRequiredPermissions()) {
            //startActivity(Intent(this, IntroActivity::class.java))
            //finish()
        }

        storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var storageList = arrayListOf<String>()
        storageManager.storageVolumes.forEach {
            storageList.add(it.mediaStoreVolumeName?: it.uuid.toString())
            if(it.isPrimary) {
                selectedStorage = it.mediaStoreVolumeName?: it.uuid.toString()
            }
        }

        val dropdown = (binding.dropdown as MaterialAutoCompleteTextView)
        dropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, storageList))
        dropdown.setText(selectedStorage, false)
        binding.dropdown.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            selectedStorage = storageList[position]
            triggerDataUpdate()
        }
        if(storageManager.storageVolumes.size==1){
            binding.storageSelector.visibility = View.GONE
        }

        triggerDataUpdate()
    }

    fun triggerDataUpdate() {

        runOnUiThread {
            binding.folders.visibility = View.INVISIBLE
            binding.loading.visibility = View.VISIBLE
        }

        lastScanStarted = System.currentTimeMillis()
        val reciever = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                CoroutineScope(Dispatchers.IO).launch{
                    ScanService.getResult()?.let { scanComplete(it) }
                    Log.e(tag(), "Scanning and processing took: ${System.currentTimeMillis()-lastScanStarted}ms")
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(reciever, IntentFilter(SCAN_COMPLETE))
        val service = Intent(this, ScanService::class.java)
        service.putExtra(SCAN_STORAGE, selectedStorage)
        startForegroundService(service)
    }

    override fun onBackPressed() {
        if (currentElement != rootElement) {
            currentElement?.parent?.let { showFolder(it) }
        } else {
            super.onBackPressed()
        }
    }

    fun updateStaticElements(currentRoot: StoragePrototype?, rootTotal: Long, rootUnused: Long) {
        val rootFree = rootUnused.div(rootTotal.toDouble())

        if(currentRoot != null) {
            val currentlyUsed = currentRoot.getCalculatedSize().div(rootTotal.toDouble())
            binding.usedText.text = readableFileSize(currentRoot.getCalculatedSize())
            ObjectAnimator
                .ofInt(binding.dataUsage, "progress", (currentlyUsed*100).toInt())
                .setDuration(300)
                .start()
        } else {
            binding.dataUsage.progress = 0
        }

        binding.freeText.text = readableFileSize(rootUnused)
        ObjectAnimator
            .ofInt(binding.freeUsage, "progress", (rootFree*100).toInt())
            .setDuration(300)
            .start()
    }

    fun showFolder(currentRoot: StoragePrototype) {

        currentElement = currentRoot

        //first, calculate percentages.
        val max = currentRoot.getCalculatedSize()
        currentRoot.getChildren().forEach {
            val percentage = (it.getCalculatedSize().toFloat()/max.toFloat())
            it.percent = (percentage*100).toInt()
        }
        //second, sort children
        val l = currentElement!!.getChildren().sortedWith(compareBy{ list -> list.getCalculatedSize()})
        currentElement!!.clearChildren()
        currentElement!!.getChildren().addAll(l.reversed())

        val recyclerView = binding.folders

        registerForContextMenu(recyclerView)



        val animation: LayoutAnimationController = AnimationUtils.loadLayoutAnimation(this, R.anim.recyclerview_animation)
        recyclerView.layoutAnimation = animation
        recyclerView.layoutManager = LinearLayoutManager(this)
        val recyclerViewAdapter = RecyclerViewAdapter(this, currentRoot.getChildren(), this)
        recyclerView.adapter = recyclerViewAdapter
    }


    fun printDepthFirst(path: String, storageElementEntry: StoragePrototype) {

        if(storageElementEntry.getChildren().size != 0) {
            storageElementEntry.getChildren().forEach {
                printDepthFirst(path+"/"+it.name, it)
            }
        } else {
            Log.e(tag(), path+" - "+ readableFileSize(storageElementEntry.getCalculatedSize()))
        }
    }

    override fun changeFolder(folder: StoragePrototype) {
        showFolder(folder)
    }

    override fun scanComplete(result: StorageResult) {

        runOnUiThread{
            rootElement = result.rootElement
            if(rootElement != null) {
                binding.folders.visibility = View.VISIBLE
                binding.loading.visibility = View.INVISIBLE
                showFolder(rootElement!!)
                updateStaticElements(rootElement!!, result.total, result.free)
            }
        }
    }

}
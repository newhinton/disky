package de.felixnuesse.disky

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Environment.MEDIA_UNMOUNTED
import android.os.storage.StorageManager
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import de.felixnuesse.disky.IntroActivity.Companion.INTRO_PREFERENCES
import de.felixnuesse.disky.IntroActivity.Companion.intro_v1_0_0_completed
import de.felixnuesse.disky.background.ScanService
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_ABORTED
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_COMPLETE
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_REFRESH_REQUESTED
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_PROGRESSED
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_STORAGE
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_SUBDIR
import de.felixnuesse.disky.databinding.ActivityMainBinding
import de.felixnuesse.disky.extensions.getAppname
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageResult
import de.felixnuesse.disky.model.StorageType
import de.felixnuesse.disky.scanner.ScanCompleteCallback
import de.felixnuesse.disky.ui.BottomSheet
import de.felixnuesse.disky.ui.ChangeFolderCallback
import de.felixnuesse.disky.ui.RecyclerViewAdapter
import de.felixnuesse.disky.utils.PermissionManager
import de.felixnuesse.disky.worker.WorkerManager
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

    companion object {
        const val APP_PREFERENCES = "APP_PREFERENCES"
        const val APP_PREFERENCE_SORTORDER = "APP_PREFERENCE_SORTORDER"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val sharedPref = applicationContext.getSharedPreferences(INTRO_PREFERENCES, Context.MODE_PRIVATE)
        val isIntroComplete = sharedPref.getBoolean(intro_v1_0_0_completed, false)
        if (!isIntroComplete) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }

        if(!PermissionManager(this).hasAllRequiredPermissions()) {
            // todo: implement runtime intro for removed permissions
            //startActivity(Intent(this, IntroActivity::class.java))
            //finish()
        }


        storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val storageList = arrayListOf<String>()
        storageManager.storageVolumes.forEach {

            if(it.state == MEDIA_UNMOUNTED) {
                return@forEach
            }

            storageList.add(it.getDescription(this))
            if(it.isPrimary) {
                selectedStorage = it.getDescription(this)
            }
        }

        val dropdown = (binding.dropdown as MaterialAutoCompleteTextView)
        dropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, storageList))
        dropdown.setText(selectedStorage, false)
        binding.dropdown.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            selectedStorage = storageList[position]
            binding.removableStorageWarning.visibility = View.GONE
            triggerDataUpdate()
        }
        if(storageList.size==1){
            binding.storageSelector.visibility = View.GONE
        }

        registerReciever()
        if(isIntroComplete) {
            //triggerDataUpdate()
            WorkerManager().scheduleDaily(this)
        }
    }

    fun registerReciever() {
        val reciever = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if(intent.action == SCAN_ABORTED) {
                    binding.progressLabel.text = "Scan was aborted"
                    return
                }
                if(intent.action == SCAN_REFRESH_REQUESTED) {
                    requestDataRefresh()
                }
                if(intent.action == SCAN_PROGRESSED) {
                    var progress = intent.getIntExtra(SCAN_PROGRESSED, 0)
                    binding.progressIndicator.isIndeterminate = false
                    binding.progressIndicator.progress = progress
                    binding.progressLabel.text = "$progress%"
                }
                if(intent.action == SCAN_COMPLETE) {
                    CoroutineScope(Dispatchers.IO).launch{
                        ScanService.getResult()?.let { scanComplete(it) }
                        Log.e(tag(), "Scanning and processing took: ${System.currentTimeMillis()-lastScanStarted}ms")
                    }
                }
            }
        }
        val filter = IntentFilter(SCAN_COMPLETE)
        filter.addAction(SCAN_ABORTED)
        filter.addAction(SCAN_REFRESH_REQUESTED)
        filter.addAction(SCAN_PROGRESSED)
        LocalBroadcastManager.getInstance(this).registerReceiver(reciever, filter)
    }

    fun triggerDataUpdate() {
        Log.e(tag(), "trigger update!")
        runOnUiThread {
            binding.folders.visibility = View.INVISIBLE
            binding.loading.visibility = View.VISIBLE
            fadeTextview(getString(R.string.calculating), binding.freeText)
            fadeTextview(getString(R.string.calculating), binding.usedText)

            val primaryColor = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryDark, primaryColor, true)
            val targetColor = Color.valueOf(Color.parseColor("#FF00FF"))

            binding.lottie.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR
            ) {
                val svg = Color.valueOf(it.startValue)
                if(svg == targetColor) {
                    primaryColor.data
                } else {
                    it.startValue
                }
            }
        }

        lastScanStarted = System.currentTimeMillis()
        val service = Intent(this, ScanService::class.java)
        service.putExtra(SCAN_STORAGE, selectedStorage)
        service.putExtra(SCAN_SUBDIR, currentElement?.getParentPath())
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
        if(currentRoot != null) {
            val currentlyUsed = currentRoot.getCalculatedSize().div(rootTotal.toDouble())
            fadeTextview(
                readableFileSize(currentRoot.getCalculatedSize()),
                binding.usedText
            )
            ObjectAnimator
                .ofInt(binding.dataUsage, "progress", (currentlyUsed*100).toInt())
                .setDuration(300)
                .start()
        } else {
            binding.dataUsage.progress = 0
        }

        fadeTextview(
            readableFileSize(rootUnused),
            binding.freeText
        )
    }

    fun showFolder(currentRoot: StoragePrototype) {
        currentElement = currentRoot

        if(currentRoot.parent==null) {
            fadeTextview(
                getString(R.string.uicontext_folder_rootdir),
                binding.infoText
            )
        }

        if(currentRoot.storageType == StorageType.APP) {
            fadeTextview(
                getString(
                    R.string.uicontext_folder_app,
                    getAppname(currentRoot.name, this),
                    readableFileSize(currentRoot.getCalculatedSize())
                ),
                binding.infoText
            )
        }

        if(currentRoot.storageType == StorageType.APP_COLLECTION) {
            fadeTextview(
                getString(
                    R.string.uicontext_folder_appcollection,
                    currentRoot.getChildren().size,
                    readableFileSize(currentRoot.getCalculatedSize())
                ),
                binding.infoText
            )
        }

        if(currentRoot.storageType == StorageType.FOLDER) {
            fadeTextview(
                getString(
                    R.string.uicontext_folder_folder,
                    currentRoot.name,
                    readableFileSize(currentRoot.getCalculatedSize())
                ),
                binding.infoText
            )
        }

        //first, calculate percentages.
        val max = currentRoot.getCalculatedSize()
        currentRoot.getChildren().forEach {
            val percentage = (it.getCalculatedSize().toFloat()/max.toFloat())
            it.percent = (percentage*100).toInt()
        }

        val sharedPref = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val sortbySize = sharedPref.getInt(APP_PREFERENCE_SORTORDER, 0) == 0 // 0 is size. If we get more, we need to decide here how to sort.
        //second, sort children
        val l = if (sortbySize) {
            currentElement!!.getChildren().sortedWith(compareBy{ list -> list.getCalculatedSize()})
        } else {
            currentElement!!.getChildren().sortedWith(compareBy{ list -> list.name.lowercase()}).reversed()
        }
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

    override fun changeFolder(folder: StoragePrototype) {
        showFolder(folder)
    }

    override fun scanComplete(result: StorageResult) {
        runOnUiThread{
            var internalRootElement = result.rootElement
            if(internalRootElement != null) {
                binding.folders.visibility = View.VISIBLE
                binding.loading.visibility = View.GONE


                binding.removableStorageWarning.visibility = if(result.scannedVolume?.isRemovable == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                (binding.dropdown as MaterialAutoCompleteTextView)
                    .setText(result.scannedVolume?.getDescription(this), false)
                if(!result.isPartialScan) {
                    rootElement = internalRootElement
                    showFolder(rootElement!!)
                    updateStaticElements(rootElement!!, result.total, result.free)
                } else {
                    rootElement?.mergePartialTree(internalRootElement)
                    currentElement?.let { changeFolder(it) }
                }
            }
        }
    }

    //Options Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            currentElement = rootElement
            currentElement?.let { changeFolder(it) }
            return true
        }
        if (id == R.id.action_settings) {
            val bl = BottomSheet()
            bl.show(supportFragmentManager, BottomSheet.TAG)
            return true
        }
        if (id == R.id.action_reload) {
            requestDataRefresh()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestDataRefresh() {
        when(currentElement?.storageType) {
            StorageType.APP -> {
                Toast.makeText(this, R.string.reload_blocked_because_inapps, Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, R.string.reload, Toast.LENGTH_SHORT).show()
                triggerDataUpdate()
            }
        }
    }
    private fun fadeTextview(text: String, view: TextView) {
        if(view.text == text) {
            return
        }
        view.visibility = View.VISIBLE

        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeIn.duration = 300
        fadeOut.duration = 300

        fadeOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation) {
                view.text = text
                view.startAnimation(fadeIn)
            }

        })
        view.startAnimation(fadeOut)
    }

}
package de.felixnuesse.disky.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.StorageStatsManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.felixnuesse.disky.R
import de.felixnuesse.disky.extensions.getStorageUUID
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageResult
import de.felixnuesse.disky.scanner.AppScanner
import de.felixnuesse.disky.scanner.FsScanner
import de.felixnuesse.disky.scanner.ScannerCallback
import de.felixnuesse.disky.scanner.SystemScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException


class ScanService: Service(), ScannerCallback {

    private lateinit var storageStatsManager: StorageStatsManager
    private lateinit var storageManager: StorageManager
    private lateinit var notificationManager: NotificationManager
    private var serviceRunId = 0L
    private var fsScanner: FsScanner? = null

    private var processedSize = 0L
    private var maxSize = 0L
    private var lastReportedPercentage = 0

    companion object {
        val SCAN_STORAGE = "SCAN_STORAGE"
        val SCAN_SUBDIR = "SCAN_SUBDIR"
        val SCAN_COMPLETE = "SCAN_COMPLETE"
        val SCAN_ABORTED = "SCAN_ABORTED"
        val SCAN_REFRESH_REQUESTED = "SCAN_REFRESH_REQUESTED"
        val SCAN_PROGRESSED = "SCAN_PROGRESSED"
        private val NOTIFICATION_CHANNEL_ID = "general_notification_channel"
        private val ERROR_NOTIFICATION_CHANNEL_ID = "error_notification_channel"
        private val ERROR_COPY_INTENT_ACTION = "de.felixnuesse.disky.background.ACTION_COPY_ERROR"
        private val NOTIFICATION_ID = 5691
        private val ERROR_NOTIFICATION_ID = 5692
        private val CLIPBOARD_INTENT_ID = 5693
        private var storageResult: StorageResult? = null

        /**
         * This method is destructive. The result can be fetched only once.
         * If it has been fetched, it is discarded and a new one needs to be requested.
         * This way, we guarantee somewhat recent data.
         */
        fun getResult(): StorageResult? {
            var tempRes = storageResult
            storageResult = null
            return tempRes
        }

    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        storageStatsManager = getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val message = getString(R.string.foreground_service_notification_starting_message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, getNotification(message, message).build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, getNotification(message, message).build())
        }


        val thisServiceRunId = System.currentTimeMillis()
        if(serviceRunId != 0L) {
            stopScan()
        }
        serviceRunId = thisServiceRunId

        processedSize = 0L

        val context = this
        CoroutineScope(Dispatchers.IO).launch{
            val now = System.currentTimeMillis()
            val storageToScan = intent?.getStringExtra(SCAN_STORAGE)
            val subfolder = intent?.getStringExtra(SCAN_SUBDIR)?: ""
            if(storageToScan.isNullOrBlank()) {
                Log.e(tag(), "No valid storage name was provided!")
                return@launch
            }

            try {
                val result = scan(storageToScan, serviceRunId, subfolder)
                Log.e(tag(), "Scanning took: ${System.currentTimeMillis()-now}ms ${wasStopped(thisServiceRunId)}")
                if(wasStopped(thisServiceRunId)) {
                    val resultIntent = Intent(SCAN_ABORTED)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
                    Log.e(tag(), "Scan was prematurely stopped!")
                } else {
                    storageResult = result
                    val resultIntent = Intent(SCAN_COMPLETE)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
                }
            } catch (e: Exception) {
                showErrorNotification(e)
                val resultIntent = Intent(SCAN_ABORTED)
                LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
            }

            finishService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun wasStopped(id: Long): Boolean {
        return serviceRunId != id
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.foreground_service_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)

        val error_channel = NotificationChannel(
            ERROR_NOTIFICATION_CHANNEL_ID,
            getString(R.string.error_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(error_channel)

    }

    private fun getNotification(message: String, bigmessage: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigmessage))
            .setSmallIcon(R.drawable.icon_servicelogo)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    //Todo: Migrate this out into its own class
    private fun showErrorNotification(exception: Exception) {
        val error = NotificationCompat.Builder(this, ERROR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.error_notification_title))
            .setContentText(getString(R.string.error_notification_channel_message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(exception.message.toString()))
            .setSmallIcon(R.drawable.round_running_with_errors_24)


        val clipboardReciever = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                copyToClipboard("Error Message", exception.stackTraceToString())
            }
        }

        val intentFilter = IntentFilter(ERROR_COPY_INTENT_ACTION)
        ContextCompat.registerReceiver(this, clipboardReciever, intentFilter, ContextCompat.RECEIVER_EXPORTED)

        val copyIntent = PendingIntent.getBroadcast(
            this,
            CLIPBOARD_INTENT_ID,
            Intent(ERROR_COPY_INTENT_ACTION),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        error.addAction(
            R.drawable.icon_copy,
            getString(R.string.error_notification_action_copy),
            copyIntent
        )

        notificationManager.notify(ERROR_NOTIFICATION_ID, error.build())
    }

    private fun copyToClipboard(label: String, content: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
    }

    private fun scan(storage: String, id: Long, subpath: String): StorageResult? {
        val selectedStorage = findStorageByNameOrUUID(storage)
        val rootElement: StoragePrototype?

        maxSize = selectedStorage?.let { getTotalSpace(it) }?:0L
        val subfolder = subpath.replace(selectedStorage?.directory!!.absolutePath+"/", "")

        fsScanner = FsScanner(this)
        if(selectedStorage.directory == null) {
            Log.e(tag(), "There was an error loading data!")
            return null
        }
        rootElement = fsScanner?.scan(selectedStorage.directory!!, subfolder)
        if(wasStopped(id)) {
            return null
        }

        val isAppfolderUpdate = subfolder.isBlank() || subfolder == getString(R.string.apps)
        // Dont scan for system and apps on external sd card
        if(selectedStorage.isPrimary && rootElement != null) {
            if(isAppfolderUpdate) {
                AppScanner(this, this).scanApps(rootElement, selectedStorage)
                if(wasStopped(id)) {
                    return null
                }
            }
            if (subfolder.isBlank()) {
                SystemScanner(this, this).scanApps(rootElement, getTotalSpace(selectedStorage), getFreeSpace(selectedStorage))
                if(wasStopped(id)) {
                    return null
                }
            }
        }

        val result = StorageResult()
        result.scannedVolume = selectedStorage
        result.rootElement = rootElement
        result.free = getFreeSpace(selectedStorage)
        result.used = rootElement?.getCalculatedSize()?: 0
        result.total = getTotalSpace(selectedStorage)
        result.isPartialScan = subfolder.isNotBlank()
        return result
    }

    private fun getTotalSpace(selectedStorage: StorageVolume): Long {
        return try {
            val uuid = getStorageUUID(selectedStorage)
            uuid?.let { storageStatsManager.getTotalBytes(it) }?: 0
        } catch (e: IOException) {
            0
        }
    }

    private fun getFreeSpace(selectedStorage: StorageVolume): Long {
        return try {
            val uuid = getStorageUUID(selectedStorage)
            uuid?.let { storageStatsManager.getFreeBytes(it) }?: 0
        } catch (e: IOException) {
            0
        }
    }

    private fun findStorageByNameOrUUID(name: String): StorageVolume? {
        storageManager.storageVolumes.forEach {
            if((name == it.getDescription(this)) or (name == it.uuid.toString())){
                return it
            }
        }
        return null
    }

    override fun currentlyScanning(item: String) {
        val shorttext = getString(R.string.foreground_service_notification_short_message, lastReportedPercentage)
        val longtext = getString(
            R.string.foreground_service_notification_long_message,
            lastReportedPercentage,
            item
        )
        val notification = getNotification(shorttext, longtext)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    override fun foundLeaf(size: Long) {

        processedSize += size
        val perc = ((processedSize.div(maxSize.toFloat()))*100).toInt()

        if(perc != lastReportedPercentage) {
            lastReportedPercentage = perc
            val progress = Intent(SCAN_PROGRESSED)
            progress.putExtra(SCAN_PROGRESSED, perc)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(progress)
        }

    }

    private fun stopScan() {
        Log.e(tag(), "Stop Scan!")
        serviceRunId = 0
        fsScanner?.stopped = true
        finishService()
    }

    /**
     * THis needs to be called, because the service is not recreated when
     * restarted. That means that wasStopped will be true, and the result never returns.
     *
     * Todo: This hack is still broken. It might be that the older thread takes longer than the new one.
     *       After the newer one finishes, it re-enables the old one, and it might then deliver it's result.
     */
    private fun finishService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
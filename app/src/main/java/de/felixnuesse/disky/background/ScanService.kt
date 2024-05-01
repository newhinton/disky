package de.felixnuesse.disky.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.core.app.NotificationCompat
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

    companion object {
        val SCAN_STORAGE = "SCAN_STORAGE"
        val SCAN_SUBDIR = "SCAN_SUBDIR"
        val SCAN_COMPLETE = "SCAN_COMPLETE"
        val SCAN_ABORTED = "SCAN_ABORTED"
        val SCAN_REFRESH_REQUESTED = "SCAN_REFRESH_REQUESTED"
        private val NOTIFICATION_CHANNEL_ID = "general_notification_channel"
        private val NOTIFICATION_ID = 5691
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

        val message = getString(R.string.foreground_service_notification_description)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, getNotification(message).build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, getNotification(message).build())
        }


        val thisServiceRunId = System.currentTimeMillis()
        if(serviceRunId != 0L) {
            stopScan()
        }
        serviceRunId = thisServiceRunId


        val context = this
        CoroutineScope(Dispatchers.IO).launch{
            val now = System.currentTimeMillis()
            val storageToScan = intent?.getStringExtra(SCAN_STORAGE)
            val subfolder = intent?.getStringExtra(SCAN_SUBDIR)?: ""
            if(storageToScan.isNullOrBlank()) {
                Log.e(tag(), "No valid storage name was provided!")
                return@launch
            }
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
    }

    private fun getNotification(message: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(message)
            .setSmallIcon(R.drawable.icon_servicelogo)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    private fun scan(storage: String, id: Long, subpath: String): StorageResult? {
        val selectedStorage = findStorageByNameOrUUID(storage)
        val rootElement: StoragePrototype?

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
                AppScanner(this).scanApps(rootElement, selectedStorage)
                if(wasStopped(id)) {
                    return null
                }
            }
            if (subfolder.isBlank()) {
                SystemScanner(this).scanApps(rootElement, getTotalSpace(selectedStorage), getFreeSpace(selectedStorage))
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
        val notification = getNotification(item)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
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
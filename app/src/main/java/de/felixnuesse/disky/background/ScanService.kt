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
import java.util.UUID


class ScanService: Service(), ScannerCallback {

    private lateinit var storageStatsManager: StorageStatsManager
    private lateinit var storageManager: StorageManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        val SCAN_STORAGE = "SCAN_STORAGE"
        val SCAN_COMPLETE = "SCAN_COMPLETE"
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

        val context = this
        CoroutineScope(Dispatchers.IO).launch{
            val now = System.currentTimeMillis()
            val result = scan(intent?.getStringExtra(SCAN_STORAGE))
            Log.e(tag(), "Scanning took: ${System.currentTimeMillis()-now}ms")
            storageResult = result
            val resultIntent = Intent(SCAN_COMPLETE)
            LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        return super.onStartCommand(intent, flags, startId)
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

    private fun scan(storage: String?): StorageResult? {
        val selectedStorage = findStorageByNameOrUUID(storage)
        val rootElement: StoragePrototype

        val scanner = FsScanner(this, this)
        if(selectedStorage?.directory == null) {
            Log.e(tag(), "There was an error loading data!")
            return null
        }
        rootElement = scanner.scan(selectedStorage.directory!!)
        AppScanner(this).scanApps(rootElement)
        SystemScanner(this).scanApps(rootElement, getTotalSpace(selectedStorage), getFreeSpace(selectedStorage))

        val result = StorageResult()
        result.rootElement = rootElement
        result.free = getFreeSpace(selectedStorage)
        result.used = rootElement.getCalculatedSize()
        result.total = getTotalSpace(selectedStorage)
        return result
    }

    private fun getTotalSpace(selectedStorage: StorageVolume): Long {
        // Assume Default storage if uuid invalid
        val uuid = if(selectedStorage.uuid != null) {
            UUID.fromString(selectedStorage.uuid)
        } else {
            StorageManager.UUID_DEFAULT
        }

        return storageStatsManager.getTotalBytes(uuid)
    }

    private fun getFreeSpace(selectedStorage: StorageVolume): Long {
        // Assume Default storage if uuid invalid
        val uuid = if(selectedStorage.uuid != null) {
            UUID.fromString(selectedStorage.uuid)
        } else {
            StorageManager.UUID_DEFAULT
        }

        return storageStatsManager.getFreeBytes(uuid)
    }

    private fun findStorageByNameOrUUID(name: String?): StorageVolume? {
        storageManager.storageVolumes.forEach {
            if(name.isNullOrBlank() and it.isPrimary) {
                return it
            }

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
}
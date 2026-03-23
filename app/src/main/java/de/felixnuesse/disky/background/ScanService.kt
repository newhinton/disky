package de.felixnuesse.disky.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.felixnuesse.disky.R
import de.felixnuesse.disky.background.NotificationUtils.Companion.FOREGROUND_NOTIFICATION_ID
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.scanner.ResultRepository
import de.felixnuesse.disky.scanner.Scanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ScanService: Service() {

    private lateinit var notificationUtils: NotificationUtils
    private var serviceRunId = 0L

    private var startedScan: Long = 0

    companion object {
        val SCAN_STORAGE = "SCAN_STORAGE"
        val SCAN_SUBDIR = "SCAN_SUBDIR"
        val SCAN_COMPLETE = "SCAN_COMPLETE"
        val SCAN_ABORTED = "SCAN_ABORTED"
        val SCAN_REFRESH_REQUESTED = "SCAN_REFRESH_REQUESTED"
        val SCAN_PROGRESSED = "SCAN_PROGRESSED"
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationUtils = NotificationUtils(applicationContext)

        val message = getString(R.string.foreground_service_notification_starting_message)
        val notification = notificationUtils.getNotification(message, message).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }


        startedScan = System.currentTimeMillis()

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

            try {
                val callback = UpdateCallback(applicationContext)

                val result = Scanner(context, callback).scan(storageToScan, subfolder)
                Log.e(tag(), "Scanning took: ${System.currentTimeMillis()-now}ms ${wasStopped(thisServiceRunId)}")
                if(wasStopped(thisServiceRunId)) {
                    val resultIntent = Intent(SCAN_ABORTED)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
                    Log.e(tag(), "Scan was prematurely stopped!")
                } else {
                    ResultRepository.postResult(result!!)
                    val resultIntent = Intent(SCAN_COMPLETE)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
                }
            } catch (e: Exception) {
                notificationUtils.showErrorNotification(e)
                val resultIntent = Intent(SCAN_ABORTED)
                LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent)
            }

            Log.e(tag(), "Scanning took: ${System.currentTimeMillis()-startedScan}ms")
            finishService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun wasStopped(id: Long): Boolean {
        return serviceRunId != id
    }

    private fun stopScan() {
        Log.e(tag(), "Stop Scan!")
        serviceRunId = 0
        finishService()
    }


    private fun finishService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationUtils.cancelOngoing()
    }
}
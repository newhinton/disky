package de.felixnuesse.disky.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service.NOTIFICATION_SERVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment.MEDIA_UNMOUNTED
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.disky.R
import de.felixnuesse.disky.background.ScanService
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_ABORTED
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_COMPLETE
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_PROGRESSED
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_STORAGE
import de.felixnuesse.disky.extensions.tag

class BackgroundScanWorker(private var context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "background_scan_notification_channel"
        private val NOTIFICATION_ID = 5692

    }

    override fun doWork(): Result {

        createNotificationChannel()

        Log.e(tag(), "trigger doWork!!")

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        storageManager.storageVolumes.forEach {
            if(it.state == MEDIA_UNMOUNTED) {
                return@forEach
            }

            //while(ScanService.isRunning()) {
            //    Thread.sleep(1000)
                //todo: dont run this endlessly
            //}

            if(it.isPrimary) {
                scanStorage(it)
            }
        }


        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    fun scanStorage(storageVolume: StorageVolume) {

        var lastScanStarted = System.currentTimeMillis()
        Log.e(tag(), "trigger update for: ${storageVolume.getDescription(context)}")

        val reciever = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if(intent.action == SCAN_ABORTED) {
                    //todo: show error
                    return
                }

                if(intent.action == SCAN_PROGRESSED) {
                    // no action needed
                }

                if(intent.action == SCAN_COMPLETE) {
                    ScanService.getResult()?.let {

                        val rootUsed = it.rootElement?.getCalculatedSize()?: 0L
                        val rootTotal = it.total
                        val currentlyUsed = rootUsed.div(rootTotal.toDouble()) * 100

                        // todo: make configurable
                        if(currentlyUsed > 90) {
                            showNotification("Used: $currentlyUsed %")
                        }
                    }

                    Log.e(tag(), "Scanning and processing took: ${System.currentTimeMillis()-lastScanStarted}ms")
                }
            }
        }
        val filter = IntentFilter(SCAN_COMPLETE)
        filter.addAction(SCAN_ABORTED)
        filter.addAction(SCAN_PROGRESSED)
        LocalBroadcastManager.getInstance(context).registerReceiver(reciever, filter)
        val service = Intent(context, ScanService::class.java)
        service.putExtra(SCAN_STORAGE, storageVolume.getDescription(context))
        context.startForegroundService(service)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.storagewarning_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.setSound(null, null)

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(message: String) {
        createNotificationChannel()
        val notification =  NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.storagewarning_notification_title))
            .setSmallIcon(R.drawable.icon_disc_full)
            .setContentText(message)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, notification.build())
        }
    }

}

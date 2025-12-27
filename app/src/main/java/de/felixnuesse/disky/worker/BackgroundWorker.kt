package de.felixnuesse.disky.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.disky.MainActivity
import de.felixnuesse.disky.MainActivity.Companion.APP_PREFERENCES
import de.felixnuesse.disky.R
import de.felixnuesse.disky.extensions.getFreeSpace
import de.felixnuesse.disky.extensions.getTotalSpace
import de.felixnuesse.disky.extensions.readableFileSize
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit


class BackgroundWorker (private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {


    private lateinit var notificationManager: NotificationManager

    companion object {


        private const val BACKGROUNDTASK_DISABLED = 0
        private const val BACKGROUNDTASK_HOURLY = 1
        private const val BACKGROUNDTASK_DAILY = 2
        private const val BACKGROUNDTASK_WEEKLY = 3
        private const val BACKGROUNDTASK_MONTHLY = 4

        private const val DAILY_HOUR_TO_RUN = 19



        fun now(context: Context) {
            val data = Data.Builder()
            val request = OneTimeWorkRequestBuilder<BackgroundWorker>()
            request.setInputData(data.build())
            WorkManager.getInstance(context).enqueue(request.build())
        }

        fun schedule(context: Context) {

            val manager = WorkManager.getInstance(context)
            // first, cancel all work so if it is disabled, we actually disable the check.
            manager.cancelAllWork()


            val sharedPref = context.applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
            val type = sharedPref.getInt(MainActivity.APP_PREFERENCE_LSW_TYPE, 0)


            var repeatInterval = 0L
            when(type) {
                // 0 is disabled
                BACKGROUNDTASK_DISABLED -> { return }
                // 1 is hourly
                BACKGROUNDTASK_HOURLY -> repeatInterval = 1
                // 2 is daily
                BACKGROUNDTASK_DAILY -> repeatInterval = 24
                // 3 is weekly
                BACKGROUNDTASK_WEEKLY -> repeatInterval = 168
                // 4 is monthly
                BACKGROUNDTASK_MONTHLY -> repeatInterval = 5040

            }



            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            if(type == BACKGROUNDTASK_HOURLY) {
                calendar.add(Calendar.HOUR, 1)
            } else {
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                calendar.set(Calendar.HOUR_OF_DAY, DAILY_HOUR_TO_RUN)
                if(currentHour >= DAILY_HOUR_TO_RUN) {
                    calendar.add(Calendar.HOUR, 24)
                }
            }

            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND,0)
            val delay = calendar.timeInMillis - now

            val workRequest = PeriodicWorkRequest.Builder(
                BackgroundWorker::class.java,
                repeatInterval,
                TimeUnit.HOURS
            )

            workRequest.setInitialDelay(delay, TimeUnit.MILLISECONDS)
            workRequest.build()

            manager.enqueueUniquePeriodicWork("Test Low Storage", ExistingPeriodicWorkPolicy.UPDATE, workRequest.build())
        }

        private val NOTIFICATION_CHANNEL_ID = "low_storage_notification_channel"
    }

    override fun doWork(): Result {
        val storageStatsManager = mContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val storageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        notificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager



        val sharedPref = mContext.applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val thresholds = sharedPref.getInt(MainActivity.APP_PREFERENCE_LSW_TRESHOLD, 0)

        var threshold = 0
        when(thresholds) {
            // 0 is disabled
            0 -> threshold = 5
            1 -> threshold = 10
            2 -> threshold = 15
            3 -> threshold = 20
            4 -> threshold = 25
            5 -> threshold = 30
        }

        storageManager.storageVolumes.forEach {
            val total = getTotalSpace(storageStatsManager, it)
            val free = getFreeSpace(storageStatsManager, it)
            val percentage = free.toDouble()*100 / total

            // todo: make the threshold configurable
            if(percentage <= threshold) {
                createNotificationChannel()
                notificationManager.notify(
                    getId(it.uuid?: UUID.randomUUID().toString()),
                    getNotification(it, threshold, free)
                )
            }
        }


        return Result.success()
    }

    private fun getId(uuid: String): Int {
        val bytes = uuid.toByteArray()
        return ByteBuffer.wrap(bytes).int
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            mContext.getString(R.string.low_storage_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        //channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotification(volume: StorageVolume, threshold: Int, free: Long): Notification {
        // todo: configurable threshold
        val message = mContext.getString(
            R.string.is_below_the_threshold_of,
            volume.getDescription(mContext),
            threshold
        )
        val bigmessage = mContext.getString(
            R.string.is_below_the_threshold_of_there_are_only_free,
            volume.getDescription(mContext),
            threshold,
            readableFileSize(free)
        )

        val notificationIntent = Intent(mContext, MainActivity::class.java)
        notificationIntent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val intent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(mContext.getString(R.string.low_storage_notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigmessage))
            .setSmallIcon(R.drawable.icon_servicelogo)
            .setContentIntent(intent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()
    }
}
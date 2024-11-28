package de.felixnuesse.disky.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit


class WorkerManager{


    fun scheduleDaily (context: Context) {
        // Todo:  make configurable
        val backgroundScannerConstraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()


        val now = Calendar.getInstance()
        val later = Calendar.getInstance()

        later.set(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DATE),
            21, 0,0)

        val diff = later.time.time-now.time.time
        val initialDelay = diff.div(60*1000) // 60sek in ms

        val workerRequest = PeriodicWorkRequest.Builder(BackgroundScanWorker::class.java, 24, TimeUnit.HOURS)
            .setConstraints(backgroundScannerConstraints)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueue(workerRequest)
    }


}
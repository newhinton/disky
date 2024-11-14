package de.felixnuesse.disky.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class WorkerManager{


    fun scheduleDaily (context: Context) {


        // Todo:  make configurable
        val backgroundScannerConstraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()


        // Todo: Calculate initial delay to 9 oclock
        val workerRequest = PeriodicWorkRequest.Builder(BackgroundScanWorker::class.java, 24, TimeUnit.HOURS)
            .setConstraints(backgroundScannerConstraints)
            .setInitialDelay(60, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueue(workerRequest)
    }


}
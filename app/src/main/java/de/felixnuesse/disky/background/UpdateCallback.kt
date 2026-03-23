package de.felixnuesse.disky.background

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.felixnuesse.disky.R
import de.felixnuesse.disky.background.ScanService.Companion.SCAN_PROGRESSED
import de.felixnuesse.disky.scanner.ScannerCallback

class UpdateCallback(private var mContext: Context): ScannerCallback {

    private var notificationUtils = NotificationUtils(mContext)
    private var processedSize = 0L
    private var lastReportedPercentage = 0
    private var maxSize = 0L


    private var foundLeafLastAction = System.currentTimeMillis()
    private var currentlyScanningLastAction = System.currentTimeMillis()

    override fun setMaxSize(totalSpace: Long) {
        maxSize = totalSpace
    }

    override fun currentlyScanning(item: String) {

        val now = System.currentTimeMillis()
        if((now - currentlyScanningLastAction) < 100) {
            //Log.e(tag(), "currentlyScanning, skip since last update is less than 100ms")
            return
        }

        currentlyScanningLastAction = System.currentTimeMillis()
        val shorttext = mContext.getString(R.string.foreground_service_notification_short_message, lastReportedPercentage)
        val longtext = mContext.getString(
            R.string.foreground_service_notification_long_message,
            lastReportedPercentage,
            item
        )

        notificationUtils.updateOngoing(shorttext, longtext)
    }

    override fun foundLeaf(size: Long) {
        processedSize += size
        val perc = ((processedSize.div(maxSize.toFloat()))*100).toInt()

        val now = System.currentTimeMillis()
        if(perc != 100 && (now - foundLeafLastAction) < 100) {
            // Log.e(tag(), "foundLeaf, skip since last update is less than 100ms")
            return
        }
        foundLeafLastAction = System.currentTimeMillis()

        if(perc != lastReportedPercentage) {
            lastReportedPercentage = perc
            val progress = Intent(SCAN_PROGRESSED)
            progress.putExtra(SCAN_PROGRESSED, perc)
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(progress)
        }

    }
}
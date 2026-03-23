package de.felixnuesse.disky.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.felixnuesse.disky.R

class NotificationUtils(private var mContext: Context) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "general_notification_channel"
        private const val ERROR_NOTIFICATION_CHANNEL_ID = "error_notification_channel"
        const val FOREGROUND_NOTIFICATION_ID = 5691
        const val ERROR_NOTIFICATION_ID = 5692
        private const val ERROR_COPY_INTENT_ACTION = "de.felixnuesse.disky.background.ACTION_COPY_ERROR"
        private const val CLIPBOARD_INTENT_ID = 5693

    }

    private var notificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            mContext.getString(R.string.foreground_service_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)

        val errorChannel = NotificationChannel(
            ERROR_NOTIFICATION_CHANNEL_ID,
            mContext.getString(R.string.error_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(errorChannel)
    }

    fun getNotification(message: String, bigmessage: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(mContext.getString(R.string.foreground_service_notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigmessage))
            .setSmallIcon(R.drawable.icon_servicelogo)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    fun showErrorNotification(exception: Exception) {
        val error = NotificationCompat.Builder(mContext, ERROR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(mContext.getString(R.string.error_notification_title))
            .setContentText(mContext.getString(R.string.error_notification_channel_message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(exception.message.toString()))
            .setSmallIcon(R.drawable.round_running_with_errors_24)

        val clipboardReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                copyToClipboard(exception.stackTraceToString())
            }
        }

        val intentFilter = IntentFilter(ERROR_COPY_INTENT_ACTION)
        ContextCompat.registerReceiver(mContext, clipboardReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)

        val copyIntent = PendingIntent.getBroadcast(
            mContext,
            CLIPBOARD_INTENT_ID,
            Intent(ERROR_COPY_INTENT_ACTION),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        error.addAction(
            R.drawable.icon_copy,
            mContext.getString(R.string.error_notification_action_copy),
            copyIntent
        )

        notificationManager.notify(ERROR_NOTIFICATION_ID, error.build())
    }

    private fun copyToClipboard(content: String) {
        val clipboard = mContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Error Message", content))
    }

    fun cancelOngoing() {
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
    }

    fun updateOngoing(shorttext: String, longtext: String) {
        val notification = getNotification(shorttext, longtext)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification.build())
    }
}
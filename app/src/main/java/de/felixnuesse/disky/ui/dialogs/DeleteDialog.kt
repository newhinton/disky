package de.felixnuesse.disky.ui.dialogs

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.felixnuesse.disky.R
import de.felixnuesse.disky.background.ScanService
import de.felixnuesse.disky.extensions.tag
import java.io.File

class DeleteDialog(private var mContext: Context, private var file: File) {

    fun askDelete() {
        val title = if(file.isDirectory) {
            R.string.delete_folder_title
        } else {
            R.string.delete_file_title
        }
        MaterialAlertDialogBuilder(mContext)
            .setTitle(mContext.getString(title))
            .setMessage(mContext.getString(R.string.delete_confirmation_text, file.name))
            .setPositiveButton(
                R.string.yes_delete
            ) { dialog, which ->
                try {
                    if(file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(tag(), e.message.toString())
                }
                val resultIntent = Intent(ScanService.SCAN_REFRESH_REQUESTED)
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(resultIntent)
            } // A null listener allows the button to dismiss the dialog and take no further action.
            .setNegativeButton(R.string.no_keep, null)
            .setIcon(R.drawable.icon_delete)
            .show()
    }
}
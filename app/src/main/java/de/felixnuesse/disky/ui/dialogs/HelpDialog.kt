package de.felixnuesse.disky.ui.dialogs

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.felixnuesse.disky.R

class HelpDialog(private var mContext: Context) {

    fun help(title: Int, message: Int) {
        MaterialAlertDialogBuilder(mContext)
            .setTitle(mContext.getString(title))
            .setMessage(mContext.getString(message))
            .setPositiveButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.dismiss();
            }
            .setIcon(R.drawable.icon_help)
            .show()
    }
}
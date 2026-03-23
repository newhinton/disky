package de.felixnuesse.disky.ui.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import de.felixnuesse.disky.MainActivity.Companion.APP_PREFERENCES
import de.felixnuesse.disky.MainActivity.Companion.APP_PREFERENCE_SORTORDER
import de.felixnuesse.disky.model.StoragePrototype

class SortingUtils {

    companion object {
        fun getSortedList(mContext: Context, children: ArrayList<StoragePrototype>): List<StoragePrototype> {
            val sharedPref = mContext.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)

            // 0 is size. If we get more, we need to decide here how to sort.
            val sortbySize = sharedPref.getInt(APP_PREFERENCE_SORTORDER, 0) == 0

            //second, sort children
            val sortedList = if (sortbySize) {
                children.sortedWith(compareBy { list -> list.getCalculatedSize() })
            } else {
                children.sortedWith(compareBy { list -> list.name.lowercase() }).reversed()
            }

            return sortedList.reversed()
        }
    }
}
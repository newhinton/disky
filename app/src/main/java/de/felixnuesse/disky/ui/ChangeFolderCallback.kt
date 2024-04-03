package de.felixnuesse.disky.ui

import de.felixnuesse.disky.model.StorageElementEntry

interface ChangeFolderCallback {

    fun changeFolder(folder: StorageElementEntry)
}
package de.felixnuesse.disky.ui

import de.felixnuesse.disky.model.FolderEntry

interface ChangeFolderCallback {

    fun changeFolder(folder: FolderEntry)
}
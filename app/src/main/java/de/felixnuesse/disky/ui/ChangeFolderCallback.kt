package de.felixnuesse.disky.ui

import de.felixnuesse.disky.model.StoragePrototype

interface ChangeFolderCallback {

    fun changeFolder(folder: StoragePrototype)
}
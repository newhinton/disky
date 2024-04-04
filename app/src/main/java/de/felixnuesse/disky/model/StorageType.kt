package de.felixnuesse.disky.model

enum class StorageType {

    GENERIC, FOLDER, FILE, APP_COLLECTION, APP, APP_DATA, APP_CACHE, APP_CACHE_EXTERNAL, APP_APK, OS;

    companion object {
        fun fromInt(value: Int): StorageType {
            return entries[value]
        }
    }
}
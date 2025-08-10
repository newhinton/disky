package de.felixnuesse.disky.model

enum class StorageType {

    GENERIC,
    FOLDER,
    FILE,
    APP_COLLECTION,
    APP,
    APP_DATA,
    APP_CACHE,
    APP_CACHE_EXTERNAL,
    APP_APK,
    OS,
    // this is a special item that allows the user interface to show a "no content"-row
    EMPTY,
    // this is a special item that allows the user interface to show a back button
    GO_BACK_UP;

    companion object {
        fun fromInt(value: Int): StorageType {
            return entries[value]
        }
    }
}
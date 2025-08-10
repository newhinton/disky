package de.felixnuesse.disky.model

class GoBackUp(targetFolder: StoragePrototype) : StoragePrototype("GoBackUp-Unused-Variable", StorageType.GO_BACK_UP) {
    init {
        parent = targetFolder
    }
}
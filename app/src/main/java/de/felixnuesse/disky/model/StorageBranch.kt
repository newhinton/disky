package de.felixnuesse.disky.model

import kotlinx.serialization.Serializable

@Serializable
class StorageBranch(
    var branchname: String,
    var branchType: StorageType = StorageType.GENERIC
): StoragePrototype(branchname, branchType)
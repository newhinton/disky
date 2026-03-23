package de.felixnuesse.disky.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.felixnuesse.disky.model.StorageResult

object ResultRepository {
    private val _result = MutableLiveData<StorageResult>()
    val result: LiveData<StorageResult> = _result

    fun postResult(obj: StorageResult) {
        _result.postValue(obj)
    }
}
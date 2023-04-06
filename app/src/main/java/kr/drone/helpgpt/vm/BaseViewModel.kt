package kr.drone.helpgpt.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

abstract class BaseViewModel(application: Application) : AndroidViewModel(application),
    LifecycleObserver {
    protected val _event: MutableLiveData<String> = MutableLiveData()
    val event: LiveData<String> = _event
}
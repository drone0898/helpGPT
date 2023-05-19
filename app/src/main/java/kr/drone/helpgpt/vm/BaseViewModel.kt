package kr.drone.helpgpt.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel(application: Application) : AndroidViewModel(application),
    LifecycleObserver {
    protected val _event: MutableStateFlow<String> = MutableStateFlow("")
    val event: StateFlow<String> = _event
}
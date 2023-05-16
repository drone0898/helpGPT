package kr.drone.helpgpt.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kr.drone.helpgpt.BuildConfig
import timber.log.Timber


@HiltAndroidApp
class BaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        // This will initialise Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
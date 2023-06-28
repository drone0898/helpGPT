package kr.drone.helpgpt.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TranslateScriptService: Service() {

    fun startForeground(){

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "TranslateScriptService:Start"
        const val ACTION_STOP = "TranslateScriptService:Stop"
        const val EXTRA_RESULT_DATA = "TranslateScriptService:Extra:ResultData"
    }
}
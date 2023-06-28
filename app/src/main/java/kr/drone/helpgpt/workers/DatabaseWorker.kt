package kr.drone.helpgpt.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams){
    override suspend fun doWork(): Result {
        return Result.success()
    }

    companion object {
        const val KEY_FILENAME = "GPT_DATA_FILENAME"
    }
}
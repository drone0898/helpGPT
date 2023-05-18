package kr.drone.helpgpt.domain

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kr.drone.helpgpt.data.local.LocalDataSource
import kr.drone.helpgpt.data.model.Summary
import javax.inject.Inject

@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class DefaultSummaryRepository @Inject constructor(
    private val localDataSource: LocalDataSource
): SummaryRepository {
    override suspend fun getAllSummary(): List<Summary> {
        TODO("Not yet implemented")
    }

    override suspend fun saveSummary(summary: Summary) {
        TODO("Not yet implemented")
    }
}
package kr.drone.helpgpt.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kr.drone.helpgpt.data.local.LocalDataSource
import kr.drone.helpgpt.data.model.Summary
import kr.drone.helpgpt.data.model.UserProfile
import javax.inject.Inject

class LocalRepository @OptIn(ExperimentalCoroutinesApi::class)
@Inject constructor(
    private val localDataSource: LocalDataSource
): SummaryRepository, UserProfileRepository {
    override suspend fun getAllSummary(): List<Summary> {
        TODO("Not yet implemented")
    }

    override suspend fun saveSummary(summary: Summary) {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getProfile(): UserProfile {
        return localDataSource.getUserProfile()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun saveUserProfile(profile: UserProfile){
        localDataSource.saveUserProfile(profile)
    }
}
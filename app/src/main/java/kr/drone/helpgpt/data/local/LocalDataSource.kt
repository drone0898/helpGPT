package kr.drone.helpgpt.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kr.drone.helpgpt.data.model.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
class LocalDataSource @Inject constructor(
    private val summaryDao: SummaryDao,
    private val userProfileDao: UserProfileDao) {

    suspend fun saveSummary(summary: Summary){
        return summaryDao.saveSummary(summary.asSummaryEntity())
    }

    suspend fun saveUserProfile(userProfile: UserProfile){
        return userProfileDao.saveUserProfile(userProfile.asUserProfileEntity())
    }

    suspend fun saveGptProfile(gptProfile: GptProfile){
        return userProfileDao.saveGptProfile(gptProfile.asGptProfileEntity())
    }

    suspend fun getSummaryList(): List<Summary> {
        return summaryDao.getAllSummary()
    }
}
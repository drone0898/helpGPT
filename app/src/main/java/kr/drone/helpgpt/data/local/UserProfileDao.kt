package kr.drone.helpgpt.data.local

import androidx.room.*
import kr.drone.helpgpt.data.model.GptProfile
import kr.drone.helpgpt.data.model.GptProfileEntity
import kr.drone.helpgpt.data.model.UserProfile
import kr.drone.helpgpt.data.model.UserProfileEntity

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM userProfileTable LIMIT 1")
    suspend fun getUserProfile(): UserProfile

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(userProfile: UserProfileEntity)

    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfileEntity)

    @Query("SELECT * FROM gptProfileTable")
    suspend fun getAllGptProfile(): List<GptProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGptProfile(gptProfile: GptProfileEntity)
}
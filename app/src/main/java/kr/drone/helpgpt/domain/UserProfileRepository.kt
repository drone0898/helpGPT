package kr.drone.helpgpt.domain

import kr.drone.helpgpt.data.model.UserProfile

interface UserProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveUserProfile(profile: UserProfile)
}
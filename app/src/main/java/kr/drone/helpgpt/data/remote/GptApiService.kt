package kr.drone.helpgpt.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface GptApiService {
    @GET("posts")
    suspend fun getAllPosts(
        @Query("userId") userId: Int,
    ): List<Int>

    @GET("photos")
    suspend fun getAllPhotos(
        @Query("albumId") albumId: Int,
    ): List<Int>
}
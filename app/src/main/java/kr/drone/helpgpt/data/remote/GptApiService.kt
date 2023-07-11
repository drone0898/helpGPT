package kr.drone.helpgpt.data.remote

import kr.drone.helpgpt.data.model.LectureList
import retrofit2.http.GET
import retrofit2.http.Query

interface GptApiService {
    @GET("courseList")
    suspend fun getList(
        @Query("serviceKey") serviceKey: String,
        @Query("Mobile") mobile:Int = 1
    ): LectureList
}
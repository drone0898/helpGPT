package kr.drone.helpgpt.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kr.drone.helpgpt.data.remote.ApiResult
import kr.drone.helpgpt.data.remote.GptApiService
import retrofit2.HttpException
import javax.inject.Inject

class NetworkRepository @Inject constructor(private val apiService: GptApiService) {
//    fun getAllPost(userId:Int): Flow<ApiResult<List<PostItem>>> =
//        handleFlowApi {
//            apiService.getAllPosts(userId)
//        }
//
//    suspend fun getAllPhotos(albumId: Int): Flow<ApiResult<List<PhotoItem>>> =
//        handleFlowApi {
//            apiService.getAllPhotos(albumId)
//        }

}

fun <T : Any> handleFlowApi(execute: suspend () -> T,): Flow<ApiResult<T>> = flow {
    emit(ApiResult.Loading)
    delay(1000)
    try {
        emit(ApiResult.Success(execute()))
    } catch (e: HttpException) {
        emit(ApiResult.Fail.Error(code = e.code(), message = e.message()))
    } catch (e: Exception) {
        emit(ApiResult.Fail.Exception(e = e))
    }
}

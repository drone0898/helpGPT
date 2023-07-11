package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kr.drone.helpgpt.data.model.GptProfile
import kr.drone.helpgpt.data.model.UserProfile
import kr.drone.helpgpt.domain.LocalRepository
import kr.drone.helpgpt.domain.NetworkRepository
import kr.drone.helpgpt.domain.OpenAIRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val openAIRepository: OpenAIRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    suspend fun insertTestData(apiKey:String){
        try{
            val gptProfile = GptProfile(apiKey,"gpt-3.5-turbo")
            localRepository.saveUserProfile(UserProfile("Drone0898",gptProfile))
            openAIRepository.updateApiKey(apiKey)
            Timber.d("Api key is Updated : $apiKey")
        }catch (e:Exception){
            throw e
        }
    }
}
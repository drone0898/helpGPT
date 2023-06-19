package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.drone.helpgpt.data.model.GptProfile
import kr.drone.helpgpt.data.model.UserProfile
import kr.drone.helpgpt.domain.LocalRepository
import kr.drone.helpgpt.domain.OpenAIRepository
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val openAIRepository: OpenAIRepository
) : ViewModel() {

    suspend fun insertTestData(apiKey:String){
        try{
            val gptProfile = GptProfile(apiKey,"gpt-3.5-turbo")
            localRepository.saveUserProfile(UserProfile("Drone0898",gptProfile))
            openAIRepository.updateApiKey(apiKey)
        }catch (e:Exception){
            throw e
        }
    }
}
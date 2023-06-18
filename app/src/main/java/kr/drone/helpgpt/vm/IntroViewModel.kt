package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.drone.helpgpt.domain.OpenAIRepository
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val openAIRepository: OpenAIRepository
) : ViewModel() {

    suspend fun insertTestData(){

    }
}
package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import com.aallam.openai.api.completion.TextCompletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kr.drone.helpgpt.domain.OpenAIRepository
import javax.inject.Inject

@HiltViewModel
class SummaryProcViewModel @Inject constructor(
    private val openAIRepository: OpenAIRepository
) : ViewModel() {

    suspend fun testOpenAI(){
        val completion: Flow<TextCompletion> =
            openAIRepository.createTextCompletions("hello their")
    }
}
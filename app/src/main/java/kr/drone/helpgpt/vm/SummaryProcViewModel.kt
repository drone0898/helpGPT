package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.drone.helpgpt.domain.OpenAIRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SummaryProcViewModel @Inject constructor(
    private val openAIRepository: OpenAIRepository
) : ViewModel() {

    public val res:MutableStateFlow<String> = MutableStateFlow("")

    @OptIn(BetaOpenAI::class)
    suspend fun testOpenAI(message: String): Flow<ChatCompletionChunk>{
        return openAIRepository.createChatCompletions(message)
    }
}
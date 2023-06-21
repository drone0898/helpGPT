package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.drone.helpgpt.domain.OpenAIRepository
import javax.inject.Inject

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

/*
TODO : error
// 결제수단을 추가하면 문제가 해결된다고함...
// https://platform.openai.com/account/billing/payment-methods
Caused by: io.ktor.client.plugins.ClientRequestException: Client request(POST https://api.openai.com/v1/chat/completions) invalid:
    "error": {
        "message": "You exceeded your current quota, please check your plan and billing details.",
        "type": "insufficient_quota",
        "param": null,
        "code": null
    }
}
 */
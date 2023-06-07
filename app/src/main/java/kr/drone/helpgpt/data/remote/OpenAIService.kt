package kr.drone.helpgpt.data.remote

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import kotlinx.coroutines.flow.Flow

interface OpenAIService {
    suspend fun createTextCompletion(completionRequest: CompletionRequest): TextCompletion
    suspend fun createTextCompletions(completionRequest: CompletionRequest): Flow<TextCompletion>
    @OptIn(BetaOpenAI::class)
    suspend fun createChatCompletion(chatCompletionRequest: ChatCompletionRequest): ChatCompletion
}
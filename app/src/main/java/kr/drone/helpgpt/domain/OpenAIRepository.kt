package kr.drone.helpgpt.domain

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.model.ModelId
import kotlinx.coroutines.flow.Flow
import kr.drone.helpgpt.data.remote.OpenAIService
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
class OpenAIRepository @Inject constructor(private val openAIService: OpenAIService) {
    suspend fun createTextCompletion(prompt: String, modelId: String = "gpt-3.5-turbo"): TextCompletion {
        val completionRequest = CompletionRequest(
            model = ModelId(modelId),
            prompt = prompt,
            echo = true
        )
        return openAIService.createTextCompletion(completionRequest)
    }

    suspend fun createTextCompletions(prompt: String, modelId: String = "gpt-3.5-turbo"): Flow<TextCompletion> {
        val completionRequest = CompletionRequest(
            model = ModelId(modelId),
            prompt = prompt,
            echo = true
        )
        return openAIService.createTextCompletions(completionRequest)
    }

    suspend fun createChatCompletion(userMessage: String, modelId: String = "gpt-3.5-turbo"): ChatCompletion {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelId),
            messages = listOf(ChatMessage(role = ChatRole.User, content = userMessage))
        )
        return openAIService.createChatCompletion(chatCompletionRequest)
    }
}

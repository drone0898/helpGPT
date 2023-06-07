package kr.drone.helpgpt.domain

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kr.drone.helpgpt.data.remote.ApiResult
import retrofit2.HttpException
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
class OpenAIRepository @Inject constructor(private val openAI: OpenAI) {
    suspend fun createTextCompletion(prompt: String, modelId: String = "gpt-3.5-turbo"): TextCompletion {
        val completionRequest = CompletionRequest(
            model = ModelId(modelId),
            prompt = prompt,
            echo = true
        )
        return openAI.completion(completionRequest)
    }

    suspend fun createTextCompletions(prompt: String, modelId: String = "gpt-3.5-turbo"): Flow<TextCompletion> {
        val completionRequest = CompletionRequest(
            model = ModelId(modelId),
            prompt = prompt,
            echo = true
        )
        return openAI.completions(completionRequest)
    }

    suspend fun createChatCompletion(userMessage: String, modelId: String = "gpt-3.5-turbo"): ChatCompletion {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelId),
            messages = listOf(ChatMessage(role = ChatRole.User, content = userMessage))
        )
        return openAI.chatCompletion(chatCompletionRequest)
    }

    suspend fun createChatCompletions(userMessage: String, modelId: String = "gpt-3.5-turbo"): Flow<ChatCompletionChunk> {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelId),
            messages = listOf(ChatMessage(role = ChatRole.User, content = userMessage))
        )
        return openAI.chatCompletions(chatCompletionRequest)
    }
}
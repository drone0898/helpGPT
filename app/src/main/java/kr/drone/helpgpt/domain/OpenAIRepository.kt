package kr.drone.helpgpt.domain

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.audio.Translation
import com.aallam.openai.api.audio.TranslationRequest
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.flow.*
import kr.drone.helpgpt.di.ApiKeyProvider
import okio.source
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
@OptIn(BetaOpenAI::class)
class OpenAIRepository @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) {
    private var openAI: OpenAI = createOpenAI()
    val compressedAudioFile: MutableSharedFlow<File?> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    private fun createOpenAI():OpenAI{
        return OpenAI(
            token = apiKeyProvider.apiKey,
            timeout = Timeout(socket = 60.seconds)
        )
    }
    fun updateApiKey(newApiKey: String) {
        apiKeyProvider.apiKey = newApiKey
        openAI = createOpenAI()
    }
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

    suspend fun transcriptionRequest(file:File, modelId:String = "whisper-1"): Translation {
        val request = TranslationRequest(
            audio = FileSource(name = file.name, source = file.source()),
            model = ModelId("whisper-1"),
        )
        return openAI.translation(request)
    }
}
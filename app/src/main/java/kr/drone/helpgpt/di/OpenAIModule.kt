package kr.drone.helpgpt.di

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.drone.helpgpt.data.remote.OpenAIService
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@OptIn(BetaOpenAI::class)
@Module
@InstallIn(SingletonComponent::class)
class OpenAIModule {

    @Singleton
    @Provides
    fun provideOpenAI(@Named("openAiApiKey") apiKey: String): OpenAI {
        return OpenAI(
            token = apiKey,
            timeout = Timeout(socket = 60.seconds)
        )
    }

    @Provides
    fun provideOpenAIService(openAI: OpenAI): OpenAIService {
        return object : OpenAIService {
            override suspend fun createTextCompletion(completionRequest: CompletionRequest): TextCompletion {
                return openAI.completion(completionRequest)
            }

            override suspend fun createChatCompletion(chatCompletionRequest: ChatCompletionRequest): ChatCompletion {
                return openAI.chatCompletion(chatCompletionRequest)
            }
        }
    }
}


@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {
    @Singleton
    @Provides
    @Named("openAiApiKey")
    fun provideOpenAiApiKey(): String{
        return "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"
    }
}
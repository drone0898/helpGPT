package kr.drone.helpgpt.di

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

class OpenAIFactory @Inject constructor(private val apiKeyProvider: ApiKeyProvider) {

    fun createOpenAI():OpenAI{
        return OpenAI(
            token = apiKeyProvider.apiKey,
            timeout = Timeout(socket = 60.seconds)
        )
    }
}

class ApiKeyProvider @Inject constructor() {
    var apiKey: String = ""
}

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {
    @Singleton
    @Provides
    @Named("openAiApiKey")
    fun provideOpenAiApiKey(apiKeyProvider: ApiKeyProvider): String{
        return apiKeyProvider.apiKey
    }
}
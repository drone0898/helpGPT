package kr.drone.helpgpt.di

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
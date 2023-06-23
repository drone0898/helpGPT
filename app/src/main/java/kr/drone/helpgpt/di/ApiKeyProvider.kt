package kr.drone.helpgpt.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor() {
    // apiKey의 상태가 업데이트 되기 때문에 Singleton이어야 함.
    // 또한 앱이 종료되면 apiKeyrkqt 역시 초기화되기 때문에 Provider는 항상 db에서 불러온 apiKey 최신데이터를 제공해야함
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
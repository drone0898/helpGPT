package kr.drone.helpgpt.di

import com.google.gson.JsonSyntaxException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.data.remote.GptApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


// https://velog.io/@jmseb3/Android-Sealed-Class-%EB%A5%BC-Retrofit-%ED%86%B5%EC%8B%A0with-Hilt-flow
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    fun provideBaseUrl() = ""


    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val connectionTimeOut = (1000 * 30).toLong()
        val readTimeOut = (1000 * 5).toLong()

        val interceptor = HttpLoggingInterceptor()

        HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                if (!message.startsWith("{") && !message.startsWith("[")) {
//                    Timber.tag("OkHttp").d(message)
                    return
                }
                try {
//                    Timber.tag("OkHttp").d(
//                        GsonBuilder().setPrettyPrinting().create().toJson(
//                        JsonParser().parse(message)))
                } catch (m: JsonSyntaxException) {
//                    Timber.tag("OkHttp").d(message)
                }
            }
        })

        interceptor.level = HttpLoggingInterceptor.Level.NONE

        if (BuildConfig.DEBUG) {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
            .connectTimeout(connectionTimeOut, TimeUnit.MILLISECONDS)
            .addInterceptor(interceptor)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun providePostsService(retrofit: Retrofit): GptApiService {
        return retrofit.create(GptApiService::class.java)
    }
}
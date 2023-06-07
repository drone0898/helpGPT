package kr.drone.helpgpt.di

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kr.drone.helpgpt.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


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
                    Timber.tag("OkHttp").d(message)
                    return
                }
                try {
                    Timber.tag("OkHttp").d(
                        GsonBuilder().setPrettyPrinting().create().toJson(
                            JsonParser.parseString(message)))
                } catch (m: JsonSyntaxException) {
                    Timber.tag("OkHttp").d(message)
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
    fun provideKtorClient(okHttpClient: OkHttpClient): HttpClient {
        return HttpClient(OkHttp){
            engine {
                config{
                    followRedirects(true)
                }
                preconfigured = okHttpClient
            }
        }
    }

//    fun provideKtorClient(): HttpClient {
//        return HttpClient(Android){
//            engine {
//                connectTimeout = 100_000
//                socketTimeout = 100_000
//                proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost",8080))
//            }
//        }
//    }

//    @Singleton
//    @Provides
//    fun provideRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
//        return Retrofit.Builder()
//            .client(okHttpClient)
//            .baseUrl(baseUrl)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }

//    @Singleton
//    @Provides
//    fun providePostsService(retrofit: Retrofit): GptApiService {
//        return retrofit.create(GptApiService::class.java)
//    }
}
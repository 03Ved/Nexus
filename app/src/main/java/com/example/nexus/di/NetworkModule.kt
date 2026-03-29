package com.example.nexus.di

import com.example.nexus.data.remote.api.CollegeApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * NetworkModule
 * Provides Retrofit, OkHttp, and API service instances
 * Configured with logging, timeouts, custom headers, and Gson converters
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.college-nexus.example.com/v1/"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Provides Gson instance with custom configuration
     * Handles null values, lenient parsing, and pretty printing
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    /**
     * Provides HttpLoggingInterceptor for request/response debugging
     * Logs full bodies in debug builds
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Provides OkHttpClient with interceptors and timeout configuration
     * Includes:
     * - Request/response logging (debug)
     * - Custom headers (User-Agent, Content-Type, Accept)
     * - Connection, read, and write timeouts
     * - Connection pooling for efficiency
     * - Retry on connection failure
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Add logging interceptor for debugging
            .addInterceptor(loggingInterceptor)
            // Add custom header interceptor
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "Nexus-Android/1.0")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            // Timeout configuration
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Connection pooling for performance
            .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
            // Retry on connection failure
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provides Retrofit instance configured with:
     * - OkHttp client with interceptors
     * - Gson converter factory for JSON serialization
     * - Base URL for API endpoint
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provides CollegeApiService for making API calls
     * Created from Retrofit instance
     */
    @Provides
    @Singleton
    fun provideCollegeApiService(retrofit: Retrofit): CollegeApiService {
        return retrofit.create(CollegeApiService::class.java)
    }
}
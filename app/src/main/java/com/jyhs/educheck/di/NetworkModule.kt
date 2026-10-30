package com.jyhs.educheck.di

import com.google.firebase.auth.FirebaseAuth
import com.jyhs.educheck.BuildConfig
import com.jyhs.educheck.data.remote.EdgeFunctionsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Network client for the Supabase Edge Functions (set-role, create-user, signed-url, heartbeat...).
 *
 * The Edge Functions are deployed WITHOUT Supabase JWT verification (`--no-verify-jwt`): they
 * validate the Firebase ID token themselves. That is why the client sends the Firebase token and
 * never needs a Supabase key.
 *
 * All functions are declared in EdgeFunctionsApi.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(auth: FirebaseAuth): OkHttpClient {
        // Adds the Firebase ID token (refreshed by the SDK when it expires) to every request.
        // Interceptors run on an OkHttp worker thread, so blocking here is safe.
        val firebaseToken = Interceptor { chain ->
            val user = auth.currentUser
            val token = user?.let { runBlocking { it.getIdToken(false).await().token } }
            val request = chain.request().newBuilder().apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(firebaseToken)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_FUNCTIONS_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideEdgeFunctionsApi(retrofit: Retrofit): EdgeFunctionsApi =
        retrofit.create(EdgeFunctionsApi::class.java)
}

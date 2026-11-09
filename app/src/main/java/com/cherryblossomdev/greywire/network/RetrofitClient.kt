package com.cherryblossomdev.greywire.network

import com.cherryblossomdev.greywire.BuildConfig
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

// Same client setup as the Prosaurus app (../Android network/RetrofitClient.kt).
object RetrofitClient {

    val BASE_URL = BuildConfig.BASE_URL

    // Called by AppContainer to wire up token persistence
    var tokenUpdateCallback: ((String) -> Unit)? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // Save the refreshed token if the server sent one
    private val tokenRefreshInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        response.header("X-New-Token")?.let { tokenUpdateCallback?.invoke(it) }
        response
    }

    // Tags every request for the backend's analytics. The backend only accepts
    // web / android / ios (backend/utilities/platform.js), so Greywire shows up
    // as "android" alongside Prosaurus for now.
    private val platformInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-Client-Platform", "android")
            .build()
        chain.proceed(request)
    }

    // Some backend routes read the JWT from a cookie rather than the header
    private val cookieInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val authHeader = originalRequest.header("Authorization")
        val newRequest = if (authHeader != null && authHeader.startsWith("Bearer ")) {
            originalRequest.newBuilder()
                .addHeader("Cookie", "jwtToken=${authHeader.removePrefix("Bearer ")}")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(newRequest)
    }

    // Falls back to direct resolution when emulator DNS fails
    private val fallbackDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                val addresses = InetAddress.getAllByName(hostname)
                if (addresses.isNotEmpty()) addresses.toList() else throw e
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dns(fallbackDns)
        .addInterceptor(platformInterceptor)
        .addInterceptor(cookieInterceptor)
        .addNetworkInterceptor(tokenRefreshInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    val greywireApiService: GreywireApiService = retrofit.create(GreywireApiService::class.java)
}

package com.example.siaga.api

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
//    internal const val BASE_URL = "http://10.10.171.111:8000/api/"
    internal const val BASE_URL = "https://siagakpuprovjambi.my.id/api/"

    private val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

//    private val client = OkHttpClient.Builder()
//        .addInterceptor(logging)
//        .build()

    // Konfigurasi client dengan timeout lebih panjang
    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))   // ⚡ Hindari bug HTTP/2 pada upload besar
        .addInterceptor(logging)
        .connectTimeout(90, TimeUnit.SECONDS)   // waktu maksimal koneksi ke server
        .readTimeout(120, TimeUnit.SECONDS)      // waktu maksimal menunggu respons
        .writeTimeout(180, TimeUnit.SECONDS)     // waktu maksimal upload data
        .retryOnConnectionFailure(true)         // otomatis retry jika gagal sementara
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        retrofit.create(ApiService::class.java)
    }
}
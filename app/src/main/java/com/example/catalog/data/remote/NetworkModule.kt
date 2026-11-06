package com.example.catalog.data.remote

import android.content.Context
import android.util.Log
import com.example.catalog.data.ApiMode
import com.example.catalog.data.DebugSettings
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/** Budowa klienta OkHttp i Retrofit. */
object NetworkModule {

    const val BASE_URL = "https://dummyjson.com/"
    const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Klient używany przez aplikację. Kolejność interceptorów ma znaczenie:
     * 1. HeaderInterceptor  – nagłówki aplikacji (widać je w logach),
     * 2. ChaosInterceptor   – tylko debug: symulacja 503 / timeoutu / braku sieci,
     * 3. AssetsInterceptor  – tryb OFFLINE_ASSETS: odpowiedź z assets zamiast sieci,
     * 4. HttpLoggingInterceptor – logowanie żądań i odpowiedzi,
     * + network interceptor CacheControlInterceptor i cache HTTP (patrz niżej).
     */
    fun createOkHttpClient(
        context: Context,
        debugSettings: DebugSettings,
        isDebug: Boolean,
    ): OkHttpClient {
        val appInterceptors = buildList {
            if (isDebug) add(ChaosInterceptor(mode = { debugSettings.chaosMode }))
            add(
                AssetsInterceptor(
                    isEnabled = { debugSettings.apiMode == ApiMode.OFFLINE_ASSETS },
                    readAsset = { path ->
                        context.assets.open(path).bufferedReader().use { it.readText() }
                    },
                ),
            )
            add(
                HttpLoggingInterceptor().apply {
                    level = if (isDebug) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
        }
        val client = createOkHttpClient(
            cacheDir = File(context.cacheDir, "http_cache"),
            appInterceptors = appInterceptors,
        )
        if (!isDebug) return client
        // Tylko debug: w Logcat (filtr „okhttp”) widać, czy odpowiedź przyszła z cache HTTP.
        // HttpLoggingInterceptor jest interceptorem aplikacji, więc loguje także żądania obsłużone z cache.
        return client.newBuilder().eventListener(CacheLogListener).build()
    }

    private object CacheLogListener : EventListener() {
        override fun cacheHit(call: Call, response: Response) {
            Log.i(CACHE_LOG_TAG, "CACHE HIT  ${call.request().url} (bez żądania do sieci)")
        }

        override fun cacheMiss(call: Call) {
            Log.i(CACHE_LOG_TAG, "CACHE MISS ${call.request().url}")
        }

        override fun cacheConditionalHit(call: Call, cachedResponse: Response) {
            Log.i(CACHE_LOG_TAG, "CACHE CONDITIONAL HIT ${call.request().url}")
        }
    }

    private const val CACHE_LOG_TAG = "okhttp.Cache"

    /**
     * Wersja bez Contextu (używana też w testach): [cacheDir] == null oznacza brak cache.
     * [appInterceptors] są dodawane po HeaderInterceptor, w podanej kolejności.
     */
    fun createOkHttpClient(
        cacheDir: File?,
        appInterceptors: List<Interceptor> = emptyList(),
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor())
        appInterceptors.forEach { builder.addInterceptor(it) }

        // TODO(UCZESTNIK) 2: Ustaw timeouty klienta: connect 10 s, read 15 s, write 15 s.
        // Szukaj metod builder.connectTimeout(...), readTimeout(...), writeTimeout(...) z TimeUnit.SECONDS.
        // (Druga część zadania 2 jest w HeaderInterceptor.kt.)

        // TODO(UCZESTNIK) 7: Włącz cache HTTP.
        // 1) dodaj CacheControlInterceptor jako NETWORK interceptor (addNetworkInterceptor, nie addInterceptor),
        // 2) gdy cacheDir != null, ustaw builder.cache(Cache(cacheDir, CACHE_SIZE_BYTES)).

        return builder.build()
    }

    fun createRetrofit(baseUrl: HttpUrl, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        createRetrofit(baseUrl.toHttpUrl(), client)
}

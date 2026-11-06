package com.example.catalog.data.remote

import com.example.catalog.data.ChaosMode
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Tylko debug: lokalna symulacja błędów, niezależna od serwera (działa też w trybie OFFLINE_ASSETS).
 * Bieżący tryb czytamy przy każdym żądaniu, więc zmiana w menu działa bez restartu.
 *
 * HTTP_503 → odpowiedź 503 (Retrofit rzuci HttpException),
 * TIMEOUT / NO_NETWORK → wyjątki dziedziczące po IOException.
 */
class ChaosInterceptor(
    private val mode: () -> ChaosMode,
    private val timeoutDelayMillis: Long = 2_000,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        when (mode()) {
            ChaosMode.NONE -> chain.proceed(chain.request())

            ChaosMode.HTTP_503 -> Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable (chaos)")
                .body("""{"message":"Chaos: 503"}""".toResponseBody("application/json".toMediaType()))
                .build()

            ChaosMode.TIMEOUT -> {
                // Krótkie opóźnienie, żeby było widać „Synchronizuję…”, bez czekania pełnych 15 s
                Thread.sleep(timeoutDelayMillis)
                throw SocketTimeoutException("Chaos: timeout")
            }

            ChaosMode.NO_NETWORK -> throw UnknownHostException("Chaos: brak sieci")
        }
}

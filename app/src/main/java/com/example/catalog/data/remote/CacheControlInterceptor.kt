package com.example.catalog.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network interceptor: DummyJSON nie wysyła nagłówków cache, więc dla szczegółów
 * produktu (`GET products/{id}`) sami ustawiamy `Cache-Control: public, max-age=300`.
 * Listy NIE buforujemy, bo synchronizacja ma zawsze pobierać świeże dane.
 */
class CacheControlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        // TODO(UCZESTNIK) 7: Dla GET products/{id} zwróć kopię odpowiedzi z nagłówkiem
        // "Cache-Control: public, max-age=$MAX_AGE_SECONDS" (response.newBuilder().header(...).build()).
        // Ścieżkę sprawdzisz przez chain.request().url.pathSegments, np. ["products", "1"].
        return response
    }

    companion object {
        const val MAX_AGE_SECONDS = 300
    }
}

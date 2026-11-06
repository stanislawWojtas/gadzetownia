package com.example.catalog.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprawdzarka TODO 2 (część: timeouty). */
class NetworkModuleTest {

    @Test
    fun clientHasLab3Timeouts() {
        val client = NetworkModule.createOkHttpClient(cacheDir = null)

        assertEquals("TODO 2: connectTimeout powinien wynosić 10 s", 10_000, client.connectTimeoutMillis)
        assertEquals("TODO 2: readTimeout powinien wynosić 15 s", 15_000, client.readTimeoutMillis)
        assertEquals("TODO 2: writeTimeout powinien wynosić 15 s", 15_000, client.writeTimeoutMillis)
    }

    @Test
    fun headerInterceptorIsFirst() {
        // Gotowe w obu wersjach: kolejność interceptorów aplikacji
        val client = NetworkModule.createOkHttpClient(cacheDir = null)
        assertTrue(client.interceptors.first() is HeaderInterceptor)
    }
}

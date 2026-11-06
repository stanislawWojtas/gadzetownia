package com.example.catalog.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Sprawdzarka TODO 2 (część: nagłówki). */
class HeaderInterceptorTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun addsClientAndAcceptHeaders() {
        server.enqueue(MockResponse().setBody("{}"))
        val client = OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor(clientVersion = "1.0"))
            .build()

        client.newCall(Request.Builder().url(server.url("/products")).build()).execute().close()
        val recorded = server.takeRequest()

        val xClient = recorded.getHeader("X-Client")
        assertTrue(
            "TODO 2: żądanie powinno mieć nagłówek X-Client zaczynający się od \"Gadzetownia\" (jest: $xClient)",
            xClient?.startsWith("Gadzetownia") == true,
        )
        assertEquals("TODO 2: X-Client powinien mieć postać Gadzetownia/<wersja>", "Gadzetownia/1.0", xClient)
        assertEquals(
            "TODO 2: żądanie powinno mieć nagłówek Accept: application/json",
            "application/json",
            recorded.getHeader("Accept"),
        )
    }
}

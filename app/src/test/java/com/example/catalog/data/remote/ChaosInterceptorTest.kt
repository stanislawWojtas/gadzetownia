package com.example.catalog.data.remote

import com.example.catalog.data.ChaosMode
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Gotowe w obu wersjach: lokalna symulacja błędów. */
class ChaosInterceptorTest {

    private val server = MockWebServer()
    private var mode = ChaosMode.NONE
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(ChaosInterceptor(mode = { mode }, timeoutDelayMillis = 0))
            .build()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun call() = client.newCall(Request.Builder().url(server.url("/products")).build()).execute()

    @Test
    fun none_passesThrough() {
        server.enqueue(MockResponse().setBody("{}"))
        call().use { assertEquals(200, it.code) }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun http503_returnsLocal503() {
        mode = ChaosMode.HTTP_503
        call().use { assertEquals(503, it.code) }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun timeoutAndNoNetwork_throwIOExceptions() {
        mode = ChaosMode.TIMEOUT
        assertThrows(SocketTimeoutException::class.java) { call() }
        mode = ChaosMode.NO_NETWORK
        assertThrows(UnknownHostException::class.java) { call() }
    }
}

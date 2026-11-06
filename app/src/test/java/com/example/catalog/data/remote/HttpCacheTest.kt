package com.example.catalog.data.remote

import com.example.catalog.testutil.Fixtures
import com.example.catalog.testutil.api
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Sprawdzarka TODO 7: cache HTTP dla szczegółów produktu. */
class HttpCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun secondDetailsRequest_isServedFromCache() = runTest {
        // Serwer (jak DummyJSON) NIE wysyła nagłówków cache: musi je dodać CacheControlInterceptor
        server.enqueue(Fixtures.jsonResponse("fixtures/product_1.json"))
        server.enqueue(Fixtures.jsonResponse("fixtures/product_1.json"))
        val client = NetworkModule.createOkHttpClient(cacheDir = tempFolder.newFolder("http_cache"))
        val api = server.api(client)

        val first = api.getProduct(1)
        val second = api.getProduct(1)

        assertEquals(first, second)
        assertEquals(
            "TODO 7: drugie getProduct(1) powinno przyjść z cache HTTP (Cache + network interceptor z Cache-Control)",
            1,
            server.requestCount,
        )
    }

    @Test
    fun productList_isNotCached() = runTest {
        // Gotowe zachowanie w obu wersjach: lista zawsze idzie do sieci
        server.enqueue(Fixtures.jsonResponse("fixtures/products_small.json"))
        server.enqueue(Fixtures.jsonResponse("fixtures/products_small.json"))
        val client = NetworkModule.createOkHttpClient(cacheDir = tempFolder.newFolder("http_cache_list"))
        val api = server.api(client)

        api.getProducts()
        api.getProducts()

        assertEquals("Lista produktów nie powinna być buforowana", 2, server.requestCount)
    }
}

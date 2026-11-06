package com.example.catalog.data.remote

import com.example.catalog.data.remote.dto.CategoryDto
import com.example.catalog.data.remote.dto.ProductDto
import com.example.catalog.data.remote.dto.ProductsResponseDto
import com.example.catalog.testutil.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Gotowe w obu wersjach: tryb OFFLINE_ASSETS odpowiada z plików i nie idzie do sieci. */
class AssetsInterceptorTest {

    private val server = MockWebServer()
    private var enabled = true
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(AssetsInterceptor(isEnabled = { enabled }, readAsset = Fixtures::read))
            .build()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun get(path: String): Pair<Int, String> =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().use {
            it.code to it.body!!.string()
        }

    @Test
    fun productsWithoutLimit_returnsFirst30() {
        val (code, body) = get("/products")
        val response = NetworkModule.json.decodeFromString<ProductsResponseDto>(body)

        assertEquals(200, code)
        assertEquals(30, response.products.size)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun productsWithLimitZeroAndSelect_returnsAllWithSelectedFields() {
        val (_, body) = get("/products?limit=0&select=title,price")
        val response = NetworkModule.json.decodeFromString<ProductsResponseDto>(body)
        val firstKeys = Json.parseToJsonElement(body).jsonObject["products"].toString()

        assertEquals(40, response.products.size)
        assertTrue("select powinien usuwać inne pola", !firstKeys.contains("\"description\""))
        assertTrue(firstKeys.contains("\"id\""))
    }

    @Test
    fun categoriesAndSingleProduct_areServed() {
        val categories = NetworkModule.json.decodeFromString<List<CategoryDto>>(get("/products/categories").second)
        val product = NetworkModule.json.decodeFromString<ProductDto>(get("/products/3").second)

        assertTrue(categories.isNotEmpty())
        assertEquals(3, product.id)
        assertEquals(404, get("/products/9999").first)
        assertEquals(404, get("/carts").first)
    }

    @Test
    fun disabled_goesToNetwork() {
        enabled = false
        server.enqueue(MockResponse().setBody("{}"))

        get("/products")

        assertEquals(1, server.requestCount)
    }
}

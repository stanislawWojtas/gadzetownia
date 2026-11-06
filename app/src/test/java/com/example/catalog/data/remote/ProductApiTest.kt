package com.example.catalog.data.remote

import com.example.catalog.testutil.Fixtures
import com.example.catalog.testutil.api
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Sprawdzarka TODO 1: parametry zapytania o listę produktów. */
class ProductApiTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun getProducts_requestsAllProductsWithSelectedFields() = runTest {
        server.enqueue(Fixtures.jsonResponse("fixtures/products_small.json"))

        val response = server.api().getProducts()
        val request = server.takeRequest()
        val url = request.requestUrl!!

        assertEquals("TODO 1: zła ścieżka żądania", "/products", url.encodedPath)
        assertEquals(
            "TODO 1: dodaj @Query(\"limit\") z wartością domyślną 0 (wszystkie produkty)",
            "0",
            url.queryParameter("limit"),
        )
        val select = url.queryParameter("select")
        assertNotNull("TODO 1: dodaj @Query(\"select\") z wartością domyślną LIST_FIELDS", select)
        assertTrue(
            "TODO 1: parametr select powinien zawierać id,title,price (jest: $select)",
            select!!.contains("id") && select.contains("title") && select.contains("price"),
        )
        assertEquals("Parsowanie odpowiedzi", 3, response.products.size)
    }
}

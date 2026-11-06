package com.example.catalog.data

import com.example.catalog.data.local.ProductEntity
import com.example.catalog.testutil.FakeProductDao
import com.example.catalog.testutil.Fixtures
import com.example.catalog.testutil.api
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Sprawdzarka TODO 3: API → Room oraz rozróżnienie HttpException / IOException. */
class ProductRepositoryTest {

    private val server = MockWebServer()
    private val existing = ProductEntity(
        id = 999,
        title = "Stary produkt",
        price = 1.0,
        discountPercentage = 0.0,
        rating = 3.0,
        thumbnail = null,
        category = "old",
        updatedAt = 1L,
    )

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun success_savesProductsInRoom() = runTest {
        server.enqueue(Fixtures.jsonResponse("fixtures/products_small.json"))
        val dao = FakeProductDao()
        val repository = ProductRepository(server.api(), dao, clock = { 42L })

        val result = repository.refreshProducts()

        assertEquals("TODO 3: przy odpowiedzi 200 zwróć RefreshResult.Success(liczba produktów)", RefreshResult.Success(3), result)
        assertEquals("TODO 3: produkty z API powinny trafić do DAO (dao.upsertAll)", 3, dao.all.size)
        assertEquals("TODO 3: encje powinny mieć czas z clock()", 42L, dao.all.first().updatedAt)
        assertEquals(
            "TODO 3: lista z Room (products) powinna zawierać pobrane produkty",
            3,
            repository.products.first().size,
        )
    }

    @Test
    fun http503_returnsHttpErrorAndKeepsRoomUntouched() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"down"}"""))
        val dao = FakeProductDao(listOf(existing))
        val repository = ProductRepository(server.api(), dao)

        val result = repository.refreshProducts()

        assertEquals("TODO 3: odpowiedź 503 → RefreshResult.HttpError(503) (złap HttpException)", RefreshResult.HttpError(503), result)
        assertEquals("TODO 3: przy błędzie dane w Room zostają bez zmian", listOf(existing), dao.all)
    }

    @Test
    fun disconnect_returnsNetworkError() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val dao = FakeProductDao(listOf(existing))
        val repository = ProductRepository(server.api(), dao)

        val result = repository.refreshProducts()

        assertTrue(
            "TODO 3: zerwane połączenie → RefreshResult.NetworkError (złap IOException), jest: $result",
            result is RefreshResult.NetworkError,
        )
        assertEquals("TODO 3: przy błędzie dane w Room zostają bez zmian", listOf(existing), dao.all)
    }
}

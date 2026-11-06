package com.example.catalog.data

import com.example.catalog.testutil.FakeCategoryDao
import com.example.catalog.testutil.Fixtures
import com.example.catalog.testutil.api
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Gotowy wzorzec „obok” (kategorie): działa w obu wersjach. */
class CategoryRepositoryTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun refresh_savesCategories() = runTest {
        server.enqueue(Fixtures.jsonResponse("api/categories.json"))
        val dao = FakeCategoryDao()
        val repository = CategoryRepository(server.api(), dao)

        assertTrue(repository.refreshCategories())
        assertEquals(24, dao.all.size)
        assertEquals("Beauty", repository.categories.first().first().name)
        assertEquals("/products/categories", server.takeRequest().requestUrl!!.encodedPath)
    }

    @Test
    fun serverError_returnsFalse() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val dao = FakeCategoryDao()

        assertFalse(CategoryRepository(server.api(), dao).refreshCategories())
        assertTrue(dao.all.isEmpty())
    }
}

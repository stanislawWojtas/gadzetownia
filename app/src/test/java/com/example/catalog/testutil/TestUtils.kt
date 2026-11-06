package com.example.catalog.testutil

import com.example.catalog.data.local.CategoryDao
import com.example.catalog.data.local.CategoryEntity
import com.example.catalog.data.local.ProductDao
import com.example.catalog.data.local.ProductEntity
import com.example.catalog.data.remote.NetworkModule
import com.example.catalog.data.remote.ProductApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/** Pliki z `src/test/resources` (fixture'y JSON, bez internetu). */
object Fixtures {
    fun read(path: String): String =
        requireNotNull(javaClass.classLoader?.getResource(path)) { "Brak pliku testowego: $path" }
            .readText()

    fun jsonResponse(path: String, code: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(read(path))
}

/** Prosty klient testowy: bez ponawiania połączeń, żeby DISCONNECT nie zjadał kolejnych odpowiedzi. */
fun plainClient(): OkHttpClient = OkHttpClient.Builder()
    .retryOnConnectionFailure(false)
    .build()

fun MockWebServer.api(client: OkHttpClient = plainClient()): ProductApi =
    NetworkModule.createRetrofit(url("/"), client).create(ProductApi::class.java)

/** Fałszywe DAO w pamięci: wystarczy do testów repozytorium (bez Room i bez Robolectric). */
class FakeProductDao(initial: List<ProductEntity> = emptyList()) : ProductDao {
    private val state = MutableStateFlow(initial.associateBy { it.id })

    val all: List<ProductEntity> get() = state.value.values.sortedBy { it.id }

    override fun observeAll(): Flow<List<ProductEntity>> = state.map { it.values.sortedBy { p -> p.title } }

    override fun observeByCategory(slug: String): Flow<List<ProductEntity>> =
        state.map { map -> map.values.filter { it.category == slug }.sortedBy { it.title } }

    override fun observeById(id: Int): Flow<ProductEntity?> = state.map { it[id] }

    override suspend fun upsertAll(products: List<ProductEntity>) {
        state.value = state.value + products.associateBy { it.id }
    }

    override suspend fun count(): Int = state.value.size
}

class FakeCategoryDao : CategoryDao {
    private val state = MutableStateFlow<Map<String, CategoryEntity>>(emptyMap())

    val all: List<CategoryEntity> get() = state.value.values.sortedBy { it.slug }

    override fun observeAll(): Flow<List<CategoryEntity>> = state.map { it.values.sortedBy { c -> c.name } }

    override suspend fun upsertAll(categories: List<CategoryEntity>) {
        state.value = state.value + categories.associateBy { it.slug }
    }
}

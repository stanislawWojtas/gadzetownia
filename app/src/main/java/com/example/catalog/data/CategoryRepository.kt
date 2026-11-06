package com.example.catalog.data

import com.example.catalog.core.Category
import com.example.catalog.data.local.CategoryDao
import com.example.catalog.data.mappers.toDomain
import com.example.catalog.data.mappers.toEntity
import com.example.catalog.data.remote.ProductApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

/**
 * GOTOWY wzorzec „obok” dla zadania 3: API → mapowanie → upsert do Room, try/catch.
 * Celowo prostszy: zwraca tylko true/false i nie rozróżnia kodów HTTP.
 * Kategorie odświeżamy bezpośrednio z ViewModelu, bez WorkManagera.
 */
class CategoryRepository(
    private val api: ProductApi,
    private val dao: CategoryDao,
) {

    val categories: Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun refreshCategories(): Boolean =
        try {
            val categories = api.getCategories()
            dao.upsertAll(categories.map { it.toEntity() })
            true
        } catch (e: IOException) {
            false
        } catch (e: HttpException) {
            false
        }
}

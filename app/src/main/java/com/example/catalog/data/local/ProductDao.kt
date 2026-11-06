package com.example.catalog.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Interfejs DAO: w testach JVM podajemy prostą fałszywą implementację (bez Robolectric). */
@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY title")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :slug ORDER BY title")
    fun observeByCategory(slug: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Int): Flow<ProductEntity?>

    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

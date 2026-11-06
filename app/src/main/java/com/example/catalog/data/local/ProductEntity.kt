package com.example.catalog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val thumbnail: String?,
    val category: String,
    /** Czas zapisu w bazie (ms), przydatny do debugowania synchronizacji. */
    val updatedAt: Long,
)

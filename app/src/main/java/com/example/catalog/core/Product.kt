package com.example.catalog.core

/** Model domenowy produktu: to, co pokazuje lista (dane z Room). */
data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val thumbnail: String?,
    val category: String,
)

/** Dodatkowe pola doczytywane z sieci na ekranie szczegółów (GET products/{id}). */
data class ProductDetails(
    val id: Int,
    val description: String?,
    val brand: String?,
    val stock: Int?,
    val availabilityStatus: String?,
    val images: List<String>,
    val tags: List<String>,
)

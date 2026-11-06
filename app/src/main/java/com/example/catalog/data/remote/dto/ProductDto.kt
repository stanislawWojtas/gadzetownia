package com.example.catalog.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Produkt w formacie DummyJSON. Część pól bywa pusta (np. `brand`) albo jest pomijana
 * przez parametr `select`, dlatego wszystko poza `id` i `title` ma wartość domyślną.
 */
@Serializable
data class ProductDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val price: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int? = null,
    val brand: String? = null,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    val availabilityStatus: String? = null,
    val tags: List<String> = emptyList(),
)

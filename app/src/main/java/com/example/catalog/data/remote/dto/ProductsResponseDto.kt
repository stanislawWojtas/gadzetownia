package com.example.catalog.data.remote.dto

import kotlinx.serialization.Serializable

/** Odpowiedź `GET products`: `{ "products": [...], "total": 194, "skip": 0, "limit": 30 }`. */
@Serializable
data class ProductsResponseDto(
    val products: List<ProductDto> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0,
)

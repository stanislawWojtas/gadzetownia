package com.example.catalog.data.remote.dto

import kotlinx.serialization.Serializable

/** Element odpowiedzi `GET products/categories`: `{ "slug", "name", "url" }`. */
@Serializable
data class CategoryDto(
    val slug: String,
    val name: String,
    val url: String? = null,
)

package com.example.catalog.data.mappers

import com.example.catalog.core.Category
import com.example.catalog.core.Product
import com.example.catalog.core.ProductDetails
import com.example.catalog.data.local.CategoryEntity
import com.example.catalog.data.local.ProductEntity
import com.example.catalog.data.remote.dto.CategoryDto
import com.example.catalog.data.remote.dto.ProductDto

// Mapowanie DTO → Entity → domena. GOTOWE w obu wersjach aplikacji.

fun ProductDto.toEntity(updatedAt: Long): ProductEntity = ProductEntity(
    id = id,
    title = title,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    thumbnail = thumbnail,
    category = category.orEmpty(),
    updatedAt = updatedAt,
)

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    title = title,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    thumbnail = thumbnail,
    category = category,
)

fun ProductDto.toDetails(): ProductDetails = ProductDetails(
    id = id,
    description = description,
    brand = brand,
    stock = stock,
    availabilityStatus = availabilityStatus,
    images = images,
    tags = tags,
)

fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(slug = slug, name = name)

fun CategoryEntity.toDomain(): Category = Category(slug = slug, name = name)

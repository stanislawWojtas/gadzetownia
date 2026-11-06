package com.example.catalog.data.mappers

import com.example.catalog.data.remote.NetworkModule
import com.example.catalog.data.remote.dto.CategoryDto
import com.example.catalog.data.remote.dto.ProductDto
import com.example.catalog.data.remote.dto.ProductsResponseDto
import com.example.catalog.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Gotowe w obu wersjach: parsowanie JSON i mapowanie DTO → Entity → domena. */
class MappersTest {

    @Test
    fun fullProductJson_parsesAndMapsToDomain() {
        val dto = NetworkModule.json.decodeFromString<ProductDto>(Fixtures.read("fixtures/product_1.json"))
        val entity = dto.toEntity(updatedAt = 7L)
        val product = entity.toDomain()

        assertEquals(1, product.id)
        assertEquals(dto.title, product.title)
        assertEquals(dto.price, product.price, 0.0)
        assertEquals("beauty", product.category)
        assertEquals(7L, entity.updatedAt)

        val details = dto.toDetails()
        assertEquals("Essence", details.brand)
        assertEquals(dto.images, details.images)
    }

    @Test
    fun selectedFieldsOnly_useDefaults() {
        // Po `select` brakuje m.in. brand, description, images
        val json = """{"id":5,"title":"Pad","price":10.5,"category":"tablets","unknownField":true}"""
        val dto = NetworkModule.json.decodeFromString<ProductDto>(json)

        assertNull(dto.brand)
        assertEquals(emptyList<String>(), dto.images)
        assertEquals(0.0, dto.rating, 0.0)
    }

    @Test
    fun productListResponse_parses() {
        val response = NetworkModule.json.decodeFromString<ProductsResponseDto>(Fixtures.read("api/products.json"))
        assertEquals(40, response.products.size)
    }

    @Test
    fun category_mapsToDomain() {
        val category = CategoryDto(slug = "laptops", name = "Laptops", url = "x").toEntity().toDomain()
        assertEquals("laptops", category.slug)
        assertEquals("Laptops", category.name)
    }
}

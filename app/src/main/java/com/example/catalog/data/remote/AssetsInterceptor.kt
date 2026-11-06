package com.example.catalog.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Tryb OFFLINE_ASSETS: odpowiada plikami z `assets/api/` i NIE idzie do sieci.
 * Dzięki temu aplikacja i zadanie działają nawet wtedy, gdy DummyJSON jest niedostępny.
 *
 * - `/products`            → `api/products.json` (z obsługą `limit`, `skip` i `select` jak w API),
 * - `/products/categories` → `api/categories.json`,
 * - `/products/{id}`       → jeden obiekt wycięty z `products.json`,
 * - inna ścieżka           → 404.
 */
class AssetsInterceptor(
    private val isEnabled: () -> Boolean,
    private val readAsset: (String) -> String,
) : Interceptor {

    private val allProducts: List<JsonObject> by lazy {
        NetworkModule.json.parseToJsonElement(readAsset(PRODUCTS_FILE))
            .jsonObject.getValue("products").jsonArray
            .map { it.jsonObject }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isEnabled()) return chain.proceed(chain.request())
        val request = chain.request()
        val segments = request.url.pathSegments.filter { it.isNotEmpty() }

        return when {
            segments == listOf("products") -> ok(request, productList(request).toString())
            segments == listOf("products", "categories") -> ok(request, readAsset(CATEGORIES_FILE))
            segments.size == 2 && segments[0] == "products" && segments[1].toIntOrNull() != null -> {
                val id = segments[1].toInt()
                val product = allProducts.firstOrNull { it["id"]?.jsonPrimitive?.int == id }
                if (product != null) ok(request, product.toString()) else notFound(request)
            }
            else -> notFound(request)
        }
    }

    private fun productList(request: Request): JsonObject {
        val url = request.url
        // Tak jak DummyJSON: brak limitu → 30 produktów, limit=0 → wszystkie
        val limit = url.queryParameter("limit")?.toIntOrNull() ?: DEFAULT_LIMIT
        val skip = url.queryParameter("skip")?.toIntOrNull() ?: 0
        val selected = url.queryParameter("select")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()

        val page = allProducts.drop(skip).let { if (limit > 0) it.take(limit) else it }
        val products = page.map { product ->
            if (selected == null) {
                product
            } else {
                JsonObject(product.filterKeys { it == "id" || it in selected })
            }
        }
        return JsonObject(
            mapOf<String, JsonElement>(
                "products" to JsonArray(products),
                "total" to JsonPrimitive(allProducts.size),
                "skip" to JsonPrimitive(skip),
                "limit" to JsonPrimitive(products.size),
            ),
        )
    }

    private fun ok(request: Request, body: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK (assets)")
        .header("Content-Type", JSON_MEDIA_TYPE)
        .body(body.toResponseBody(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    private fun notFound(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(404)
        .message("Not Found (assets)")
        .body("""{"message":"Not found in assets"}""".toResponseBody(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    companion object {
        const val PRODUCTS_FILE = "api/products.json"
        const val CATEGORIES_FILE = "api/categories.json"
        const val DEFAULT_LIMIT = 30
        private const val JSON_MEDIA_TYPE = "application/json"
    }
}

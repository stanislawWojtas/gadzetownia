package com.example.catalog.data.remote

import com.example.catalog.data.remote.dto.CategoryDto
import com.example.catalog.data.remote.dto.ProductDto
import com.example.catalog.data.remote.dto.ProductsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Interfejs Retrofit dla DummyJSON (https://dummyjson.com/). */
interface ProductApi {

    // region SOLUTION TODO 1
    /**
     * Lista produktów do zapisania w Room.
     * `limit=0` zwraca wszystkie produkty (bez parametru API zwraca tylko 30),
     * `select` ogranicza pola do tych, które trzymamy w bazie (mniejszy JSON).
     */
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = 0,
        @Query("select") select: String = LIST_FIELDS,
    ): ProductsResponseDto
    // endregion
    // region STUB TODO 1
    // // TODO(UCZESTNIK) 1: Pobierz WSZYSTKIE produkty i tylko potrzebne pola.
    // // Dodaj parametry @Query: "limit" (domyślnie 0 = wszystkie) oraz "select" (domyślnie LIST_FIELDS).
    // // Wzór adnotacji i suspend fun masz niżej w getCategories() i getProduct(id).
    // @GET("products")
    // suspend fun getProducts(): ProductsResponseDto
    // endregion

    /** Pełne dane jednego produktu (ekran szczegółów, buforowane w cache HTTP). */
    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDto

    /** Kategorie: GOTOWY wzorzec bez parametrów. */
    @GET("products/categories")
    suspend fun getCategories(): List<CategoryDto>

    companion object {
        /** Pola potrzebne na liście (i w Room), przekazywane w parametrze `select`. */
        const val LIST_FIELDS = "id,title,price,discountPercentage,rating,thumbnail,category"
    }
}

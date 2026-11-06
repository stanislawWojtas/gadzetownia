package com.example.catalog.data

import com.example.catalog.core.Product
import com.example.catalog.core.ProductDetails
import com.example.catalog.data.local.ProductDao
import com.example.catalog.data.mappers.toDetails
import com.example.catalog.data.mappers.toDomain
import com.example.catalog.data.mappers.toEntity
import com.example.catalog.data.remote.ProductApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

/**
 * Offline-first: UI czyta WYŁĄCZNIE z Room (Flow), a sieć tylko zasila bazę
 * w [refreshProducts]. Wyjątek: dodatkowe pola szczegółów ([fetchDetails]).
 */
class ProductRepository(
    private val api: ProductApi,
    private val dao: ProductDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    val products: Flow<List<Product>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Produkty z danej kategorii; `null` = wszystkie. */
    fun productsIn(category: String?): Flow<List<Product>> {
        val source = if (category == null) dao.observeAll() else dao.observeByCategory(category)
        return source.map { list -> list.map { it.toDomain() } }
    }

    fun product(id: Int): Flow<Product?> = dao.observeById(id).map { it?.toDomain() }

    /**
     * Pobiera katalog z API i zapisuje go w Room.
     * Przy błędzie baza zostaje nietknięta, a wynik mówi, CO poszło nie tak.
     */
    suspend fun refreshProducts(): RefreshResult {
        // region SOLUTION TODO 3
        return try {
            val response = api.getProducts()
            val now = clock()
            val entities = response.products.map { it.toEntity(updatedAt = now) }
            dao.upsertAll(entities)
            RefreshResult.Success(entities.size)
        } catch (e: HttpException) {
            // Serwer odpowiedział kodem błędu (np. 503)
            RefreshResult.HttpError(e.code())
        } catch (e: IOException) {
            // Brak sieci, timeout, zerwane połączenie.
            // CancellationException NIE jest IOException, więc anulowanie korutyny działa normalnie.
            RefreshResult.NetworkError(e)
        }
        // endregion
        // region STUB TODO 3
        // // TODO(UCZESTNIK) 3: Pobierz produkty przez api.getProducts(), zamień je na encje (toEntity(clock()))
        // // i zapisz przez dao.upsertAll(...). Zwróć RefreshResult.Success(liczba produktów).
        // // Złap OSOBNO HttpException → HttpError(e.code()) i IOException → NetworkError(e).
        // // Wzór try/catch: CategoryRepository.refreshCategories().
        // return RefreshResult.Success(0)
        // endregion
    }

    /** Pełne dane produktu (opis, marka, galeria). Ponowne wejście trafia w cache HTTP. */
    suspend fun fetchDetails(id: Int): Result<ProductDetails> =
        try {
            Result.success(api.getProduct(id).toDetails())
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        }
}

package com.example.catalog.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catalog.core.Product
import com.example.catalog.core.ProductDetails
import com.example.catalog.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stan doczytywania pełnych danych z sieci. */
sealed interface DetailsLoad {
    data object Loading : DetailsLoad
    data class Loaded(val details: ProductDetails) : DetailsLoad
    data object Unavailable : DetailsLoad
}

data class ProductDetailsUiState(
    /** Dane z Room: dostępne od razu, także offline. */
    val product: Product? = null,
    val details: DetailsLoad = DetailsLoad.Loading,
)

class ProductDetailsViewModel(
    private val productId: Int,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val details = MutableStateFlow<DetailsLoad>(DetailsLoad.Loading)

    val uiState: StateFlow<ProductDetailsUiState> =
        combine(productRepository.product(productId), details) { product, load ->
            ProductDetailsUiState(product, load)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailsUiState())

    init {
        loadDetails()
    }

    fun loadDetails() {
        details.value = DetailsLoad.Loading
        viewModelScope.launch {
            details.value = productRepository.fetchDetails(productId).fold(
                onSuccess = { DetailsLoad.Loaded(it) },
                onFailure = { DetailsLoad.Unavailable },
            )
        }
    }
}

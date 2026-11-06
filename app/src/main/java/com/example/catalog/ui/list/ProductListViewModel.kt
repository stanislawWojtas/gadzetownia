package com.example.catalog.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catalog.core.Category
import com.example.catalog.core.Product
import com.example.catalog.core.SyncStatus
import com.example.catalog.core.UiState
import com.example.catalog.data.CategoryRepository
import com.example.catalog.data.ProductRepository
import com.example.catalog.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    /** Lista zawsze z Room; zmiana kategorii przełącza źródło (flatMapLatest). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<List<Product>>> = _selectedCategory
        .flatMapLatest { category -> productRepository.productsIn(category) }
        .map { products -> if (products.isEmpty()) UiState.Empty else UiState.Success(products) }
        .catch { emit(UiState.Error(it.message ?: "Błąd bazy danych")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncStatus: StateFlow<SyncStatus> = syncScheduler.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.Idle(null))

    init {
        // Kategorie odświeżamy bezpośrednio (bez Workera): kontrast do synchronizacji produktów
        refreshCategories()
    }

    fun onSyncClicked() {
        syncScheduler.syncNow()
        if (categories.value.isEmpty()) refreshCategories()
    }

    fun onCategorySelected(slug: String?) {
        _selectedCategory.value = slug
    }

    private fun refreshCategories() {
        viewModelScope.launch { categoryRepository.refreshCategories() }
    }
}

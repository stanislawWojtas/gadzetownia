package com.example.catalog.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.catalog.AppContainer
import com.example.catalog.CatalogApp
import com.example.catalog.ui.details.ProductDetailsViewModel
import com.example.catalog.ui.list.ProductListViewModel

/** Fabryki ViewModeli korzystające z AppContainer (ręczne DI). */
object ViewModelFactories {

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as CatalogApp).container

    val productList: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = container()
            ProductListViewModel(
                productRepository = container.productRepository,
                categoryRepository = container.categoryRepository,
                syncScheduler = container.syncScheduler,
            )
        }
    }

    fun productDetails(productId: Int): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ProductDetailsViewModel(productId, container().productRepository)
        }
    }
}

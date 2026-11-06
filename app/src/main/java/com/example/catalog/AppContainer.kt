package com.example.catalog

import android.content.Context
import androidx.work.WorkManager
import com.example.catalog.data.CategoryRepository
import com.example.catalog.data.DebugSettings
import com.example.catalog.data.ProductRepository
import com.example.catalog.data.local.AppDatabase
import com.example.catalog.data.remote.NetworkModule
import com.example.catalog.data.remote.ProductApi
import com.example.catalog.sync.SyncPreferences
import com.example.catalog.sync.SyncScheduler
import okhttp3.OkHttpClient

/**
 * Ręczne wstrzykiwanie zależności: jedno miejsce, w którym składamy
 * OkHttp, Retrofit, bazę Room, repozytoria i planistę synchronizacji.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val debugSettings: DebugSettings = DebugSettings(appContext)

    val okHttpClient: OkHttpClient by lazy {
        NetworkModule.createOkHttpClient(appContext, debugSettings, BuildConfig.DEBUG)
    }

    val productApi: ProductApi by lazy {
        NetworkModule.createRetrofit(NetworkModule.BASE_URL, okHttpClient)
            .create(ProductApi::class.java)
    }

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val productRepository: ProductRepository by lazy {
        ProductRepository(productApi, database.productDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(productApi, database.categoryDao())
    }

    val syncPreferences: SyncPreferences by lazy { SyncPreferences(appContext) }

    val syncScheduler: SyncScheduler by lazy {
        SyncScheduler(WorkManager.getInstance(appContext), syncPreferences)
    }
}

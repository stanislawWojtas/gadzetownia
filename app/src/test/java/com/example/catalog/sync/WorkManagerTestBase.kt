package com.example.catalog.sync

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Wspólna konfiguracja testów WorkManagera (Robolectric + testowy WorkManager).
 * Używamy zwykłej Application, żeby nie uruchamiać CatalogApp (sieci, bazy) w testach.
 * W testowym WorkManagerze warunki (np. sieć) są domyślnie NIESPEŁNIONE.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
abstract class WorkManagerTestBase {

    protected lateinit var context: Context
    protected lateinit var workManager: WorkManager
    protected lateinit var scheduler: SyncScheduler

    @Before
    fun setUpWorkManager() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        scheduler = SyncScheduler(workManager, SyncPreferences(context))
    }
}

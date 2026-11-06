package com.example.catalog

import android.app.Application

/**
 * Klasa aplikacji: tworzy kontener zależności (ręczne DI, jak w Lab2)
 * i planuje okresową synchronizację w tle.
 */
class CatalogApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Unikalna praca okresowa: kolejne uruchomienia aplikacji jej nie dublują
        container.syncScheduler.schedulePeriodic()
    }
}

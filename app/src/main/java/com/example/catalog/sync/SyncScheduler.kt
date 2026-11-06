package com.example.catalog.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.catalog.core.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit

/** Planowanie synchronizacji w WorkManagerze i obserwacja jej statusu. */
class SyncScheduler(
    private val workManager: WorkManager,
    private val syncPreferences: SyncPreferences,
) {

    /** Status bieżącej (ostatniej) synchronizacji „na żądanie”. */
    val status: Flow<SyncStatus> =
        combine(
            workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_SYNC),
            syncPreferences.lastSyncFlow(),
        ) { infos, lastSync -> infos.firstOrNull().toSyncStatus(lastSync) }

    /** Synchronizacja na żądanie (przycisk „Synchronizuj”, pull-to-refresh, „Ponów”). */
    fun syncNow() {
        // region SOLUTION TODO 5
        // KEEP: jeśli praca już czeka albo trwa, kolejne kliknięcia jej nie dublują
        workManager.enqueueUniqueWork(UNIQUE_SYNC, ExistingWorkPolicy.KEEP, buildSyncRequest())
        // endregion
        // region STUB TODO 5
        // // TODO(UCZESTNIK) 5 (część 2/3): Kolejne kliknięcia nie mogą dublować pracy, która już czeka.
        // // Zmień politykę na taką, która ZOSTAWIA istniejącą (niezakończoną) pracę.
        // workManager.enqueueUniqueWork(UNIQUE_SYNC, ExistingWorkPolicy.REPLACE, buildSyncRequest())
        // endregion
    }

    /** Okresowa synchronizacja w tle (wołana w CatalogApp.onCreate). */
    fun schedulePeriodic() {
        // region SOLUTION TODO 6
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)
            .setConstraints(networkConstraints)
            // Pierwsze uruchomienie dopiero po interwale: katalog pobiera najpierw użytkownik („Synchronizuj”)
            .setInitialDelay(PERIODIC_HOURS, TimeUnit.HOURS)
            .addTag(TAG_SYNC)
            .build()
        // KEEP: ponowne uruchomienie aplikacji nie tworzy drugiej pracy okresowej
        workManager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        // endregion
        // region STUB TODO 6
        // // TODO(UCZESTNIK) 6: Zaplanuj UNIKALNĄ pracę okresową UNIQUE_PERIODIC (co 12 h, tylko z siecią).
        // // Zbuduj żądanie przez PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)
        // // z setConstraints(networkConstraints) i zakolejkuj przez workManager.enqueueUniquePeriodicWork(...).
        // // Którą politykę wybrać, żeby restart aplikacji nie tworzył drugiej pracy? (ExistingPeriodicWorkPolicy)
        // // Opcjonalnie: setInitialDelay(PERIODIC_HOURS, TimeUnit.HOURS), żeby pierwsze uruchomienie było po 12 h.
        // endregion
    }

    companion object {
        const val UNIQUE_SYNC = "product_sync"
        const val UNIQUE_PERIODIC = "product_sync_periodic"
        const val TAG_SYNC = "sync"
        const val MAX_ATTEMPTS = 3
        const val BACKOFF_SECONDS = 10L
        const val PERIODIC_HOURS = 12L

        /** Wymóg sieci: bez połączenia praca czeka w kolejce. */
        val networkConstraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Jednorazowe żądanie synchronizacji (osobna funkcja, żeby test mógł je sprawdzić). */
        fun buildSyncRequest(): OneTimeWorkRequest {
            // region SOLUTION TODO 5
            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .addTag(TAG_SYNC)
                .build()
            // endregion
            // region STUB TODO 5
            // // TODO(UCZESTNIK) 5 (część 1/3): Dodaj do żądania wymóg sieci (setConstraints(networkConstraints))
            // // oraz wykładniczy backoff: setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS).
            // return OneTimeWorkRequestBuilder<SyncWorker>()
            //     .addTag(TAG_SYNC)
            //     .build()
            // endregion
        }
    }
}

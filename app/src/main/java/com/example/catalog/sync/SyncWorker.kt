package com.example.catalog.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.catalog.CatalogApp
import com.example.catalog.data.RefreshResult

/**
 * Synchronizacja katalogu w tle: API → Room.
 * Decyzję success / retry / failure podejmuje czysta funkcja [toWorkerResult],
 * którą da się przetestować bez Androida.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CatalogApp).container
        val result = container.productRepository.refreshProducts()
        val finishedAt = System.currentTimeMillis()
        if (result is RefreshResult.Success) {
            container.syncPreferences.saveLastSync(result.count, finishedAt)
        }
        return result.toWorkerResult(runAttemptCount, finishedAt)
    }

    companion object {
        const val KEY_COUNT = "count"
        const val KEY_FINISHED_AT = "finished_at"
        const val KEY_ERROR = "error"
    }
}

/**
 * Zamienia wynik odświeżenia na wynik Workera.
 * [runAttemptCount] liczy się od 0 (pierwsza próba = 0).
 */
fun RefreshResult.toWorkerResult(
    runAttemptCount: Int,
    finishedAt: Long = System.currentTimeMillis(),
): ListenableWorker.Result {
    // TODO(UCZESTNIK) 4: Zwróć właściwy wynik Workera:
    // Success → Result.success(workDataOf(KEY_COUNT to count, KEY_FINISHED_AT to finishedAt)),
    // NetworkError i HttpError 5xx → Result.retry(), HttpError 4xx → Result.failure(errorData()).
    // Na ostatniej próbie (runAttemptCount >= SyncScheduler.MAX_ATTEMPTS - 1) błąd → Result.failure(errorData()).
    return ListenableWorker.Result.success()
}

/** Dane wyjściowe dla nieudanej synchronizacji (powód pokazywany w pasku statusu). */
fun RefreshResult.errorData() = workDataOf(SyncWorker.KEY_ERROR to describeError())

/** Czytelny opis błędu: inny dla HttpException (kod) i inny dla IOException (sieć). */
fun RefreshResult.describeError(): String = when (this) {
    is RefreshResult.Success -> "brak błędu"
    is RefreshResult.HttpError -> "serwer zwrócił błąd HTTP $code"
    is RefreshResult.NetworkError ->
        "problem z siecią (${cause::class.java.simpleName}: ${cause.message ?: "brak szczegółów"})"
}

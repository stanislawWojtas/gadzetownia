package com.example.catalog.sync

import androidx.work.ListenableWorker
import com.example.catalog.data.RefreshResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

/** Sprawdzarka TODO 4: decyzja success / retry / failure (czysta funkcja, bez Androida). */
class SyncWorkerResultTest {

    private val networkError = RefreshResult.NetworkError(UnknownHostException("brak sieci"))

    @Test
    fun success_returnsSuccessWithOutputData() {
        val result = RefreshResult.Success(194).toWorkerResult(runAttemptCount = 0, finishedAt = 1234L)

        assertTrue("TODO 4: Success → Result.success(...), jest: $result", result is ListenableWorker.Result.Success)
        val data = (result as ListenableWorker.Result.Success).outputData
        assertEquals("TODO 4: outputData powinno zawierać KEY_COUNT", 194, data.getInt(SyncWorker.KEY_COUNT, -1))
        assertEquals("TODO 4: outputData powinno zawierać KEY_FINISHED_AT", 1234L, data.getLong(SyncWorker.KEY_FINISHED_AT, -1))
    }

    @Test
    fun networkError_isRetried() {
        val result = networkError.toWorkerResult(runAttemptCount = 0)
        assertTrue("TODO 4: NetworkError → Result.retry(), jest: $result", result is ListenableWorker.Result.Retry)
    }

    @Test
    fun serverError_isRetried() {
        val result = RefreshResult.HttpError(503).toWorkerResult(runAttemptCount = 1)
        assertTrue("TODO 4: HttpError 5xx → Result.retry(), jest: $result", result is ListenableWorker.Result.Retry)
    }

    @Test
    fun clientError_isNotRetried() {
        val result = RefreshResult.HttpError(404).toWorkerResult(runAttemptCount = 0)
        assertTrue("TODO 4: HttpError 4xx → Result.failure() (ponawianie nic nie da), jest: $result", result is ListenableWorker.Result.Failure)
        val reason = (result as ListenableWorker.Result.Failure).outputData.getString(SyncWorker.KEY_ERROR)
        assertTrue("TODO 4: failure powinno mieć powód w outputData (errorData())", reason?.contains("404") == true)
    }

    @Test
    fun lastAttempt_failsInsteadOfRetrying() {
        val lastAttempt = SyncScheduler.MAX_ATTEMPTS - 1
        val http = RefreshResult.HttpError(503).toWorkerResult(runAttemptCount = lastAttempt)
        val network = networkError.toWorkerResult(runAttemptCount = lastAttempt)

        assertTrue("TODO 4: na ostatniej próbie 5xx → Result.failure(), jest: $http", http is ListenableWorker.Result.Failure)
        assertTrue("TODO 4: na ostatniej próbie błąd sieci → Result.failure(), jest: $network", network is ListenableWorker.Result.Failure)
    }
}

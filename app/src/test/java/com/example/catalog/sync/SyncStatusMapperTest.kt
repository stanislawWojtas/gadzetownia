package com.example.catalog.sync

import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.example.catalog.core.LastSync
import com.example.catalog.core.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/** Sprawdzarka TODO 5 (część 3/3): WorkInfo → SyncStatus. */
class SyncStatusMapperTest {

    private val last = LastSync(count = 10, at = 100L)

    private fun info(
        state: WorkInfo.State,
        attempts: Int = 0,
        output: androidx.work.Data = androidx.work.Data.EMPTY,
    ) = WorkInfo(
        id = UUID.randomUUID(),
        state = state,
        tags = emptySet(),
        outputData = output,
        runAttemptCount = attempts,
    )

    @Test
    fun noWork_isIdle() {
        assertEquals("TODO 5: brak pracy → Idle(lastSync)", SyncStatus.Idle(last), null.toSyncStatus(last))
    }

    @Test
    fun enqueuedFirstTime_isWaitingForNetwork() {
        assertEquals(
            "TODO 5: ENQUEUED i runAttemptCount == 0 → WaitingForNetwork",
            SyncStatus.WaitingForNetwork,
            info(WorkInfo.State.ENQUEUED).toSyncStatus(last),
        )
    }

    @Test
    fun enqueuedAfterFailure_isRetrying() {
        assertEquals(
            "TODO 5: ENQUEUED i runAttemptCount == 1 → Retrying(2)",
            SyncStatus.Retrying(2),
            info(WorkInfo.State.ENQUEUED, attempts = 1).toSyncStatus(last),
        )
    }

    @Test
    fun running_isRunning() {
        assertEquals("TODO 5: RUNNING → Running", SyncStatus.Running, info(WorkInfo.State.RUNNING).toSyncStatus(last))
    }

    @Test
    fun succeeded_readsOutputData() {
        val output = workDataOf(SyncWorker.KEY_COUNT to 194, SyncWorker.KEY_FINISHED_AT to 5000L)
        assertEquals(
            "TODO 5: SUCCEEDED → Succeeded(count, at) z outputData",
            SyncStatus.Succeeded(194, 5000L),
            info(WorkInfo.State.SUCCEEDED, output = output).toSyncStatus(last),
        )
    }

    @Test
    fun failed_readsReason() {
        val output = workDataOf(SyncWorker.KEY_ERROR to "serwer zwrócił błąd HTTP 503")
        assertEquals(
            "TODO 5: FAILED → Failed(powód z outputData)",
            SyncStatus.Failed("serwer zwrócił błąd HTTP 503"),
            info(WorkInfo.State.FAILED, output = output).toSyncStatus(last),
        )
    }
}

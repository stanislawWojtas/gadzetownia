package com.example.catalog.sync

import androidx.work.WorkInfo
import com.example.catalog.core.LastSync
import com.example.catalog.core.SyncStatus

/**
 * Zamienia stan pracy WorkManagera na status pokazywany w UI.
 * `null` = nie ma (jeszcze) żadnej pracy o tej unikalnej nazwie.
 */
fun WorkInfo?.toSyncStatus(lastSync: LastSync?): SyncStatus {
    // region SOLUTION TODO 5
    if (this == null) return SyncStatus.Idle(lastSync)
    return when (state) {
        // ENQUEUED: czeka na warunki (sieć) albo na kolejną próbę po backoffie
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
            if (runAttemptCount == 0) SyncStatus.WaitingForNetwork else SyncStatus.Retrying(runAttemptCount + 1)
        WorkInfo.State.RUNNING -> SyncStatus.Running
        WorkInfo.State.SUCCEEDED -> SyncStatus.Succeeded(
            count = outputData.getInt(SyncWorker.KEY_COUNT, lastSync?.count ?: 0),
            at = outputData.getLong(SyncWorker.KEY_FINISHED_AT, lastSync?.at ?: 0L),
        )
        WorkInfo.State.FAILED -> SyncStatus.Failed(
            outputData.getString(SyncWorker.KEY_ERROR) ?: "nieznany błąd",
        )
        WorkInfo.State.CANCELLED -> SyncStatus.Idle(lastSync)
    }
    // endregion
    // region STUB TODO 5
    // // TODO(UCZESTNIK) 5 (część 3/3): Zamień WorkInfo na SyncStatus według stanu (this?.state):
    // // ENQUEUED: runAttemptCount == 0 → WaitingForNetwork, > 0 → Retrying(runAttemptCount + 1);
    // // RUNNING → Running; SUCCEEDED → Succeeded(liczba i czas z outputData); FAILED → Failed(powód z outputData);
    // // null / CANCELLED → Idle(lastSync). Klucze danych są w SyncWorker (KEY_COUNT, KEY_FINISHED_AT, KEY_ERROR).
    // return SyncStatus.Idle(lastSync)
    // endregion
}

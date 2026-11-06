package com.example.catalog.sync

import androidx.work.WorkInfo
import com.example.catalog.core.LastSync
import com.example.catalog.core.SyncStatus

/**
 * Zamienia stan pracy WorkManagera na status pokazywany w UI.
 * `null` = nie ma (jeszcze) żadnej pracy o tej unikalnej nazwie.
 */
fun WorkInfo?.toSyncStatus(lastSync: LastSync?): SyncStatus {
    // TODO(UCZESTNIK) 5 (część 3/3): Zamień WorkInfo na SyncStatus według stanu (this?.state):
    // ENQUEUED: runAttemptCount == 0 → WaitingForNetwork, > 0 → Retrying(runAttemptCount + 1);
    // RUNNING → Running; SUCCEEDED → Succeeded(liczba i czas z outputData); FAILED → Failed(powód z outputData);
    // null / CANCELLED → Idle(lastSync). Klucze danych są w SyncWorker (KEY_COUNT, KEY_FINISHED_AT, KEY_ERROR).
    return SyncStatus.Idle(lastSync)
}

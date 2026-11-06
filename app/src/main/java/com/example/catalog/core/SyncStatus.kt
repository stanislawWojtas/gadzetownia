package com.example.catalog.core

/** Ostatnia udana synchronizacja (zapisywana w SharedPreferences przez SyncWorker). */
data class LastSync(
    val count: Int,
    val at: Long,
)

/** Status synchronizacji pokazywany w pasku pod toolbarem. */
sealed interface SyncStatus {
    /** Nic się nie dzieje; [lastSync] == null oznacza „jeszcze nie synchronizowano”. */
    data class Idle(val lastSync: LastSync?) : SyncStatus

    /** Praca czeka w kolejce na spełnienie warunku (sieć). */
    data object WaitingForNetwork : SyncStatus

    data object Running : SyncStatus

    /** Poprzednia próba się nie udała, WorkManager ponowi ją z opóźnieniem (backoff). */
    data class Retrying(val attempt: Int) : SyncStatus

    data class Succeeded(val count: Int, val at: Long) : SyncStatus

    data class Failed(val reason: String) : SyncStatus
}

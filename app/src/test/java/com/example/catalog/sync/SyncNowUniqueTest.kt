package com.example.catalog.sync

import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sprawdzarka TODO 5 (część 2/3): unikalna praca, kolejne kliknięcia jej nie dublują. */
class SyncNowUniqueTest : WorkManagerTestBase() {

    @Test
    fun syncNowTwice_withoutNetwork_keepsSingleEnqueuedWork() {
        scheduler.syncNow()
        val first = workManager.getWorkInfosForUniqueWork(SyncScheduler.UNIQUE_SYNC).get()
        scheduler.syncNow()
        val infos = workManager.getWorkInfosForUniqueWork(SyncScheduler.UNIQUE_SYNC).get()

        assertEquals("TODO 5: pod nazwą UNIQUE_SYNC powinna być dokładnie jedna praca", 1, infos.size)
        assertEquals(
            "TODO 5: bez sieci praca powinna CZEKAĆ w kolejce (constraint CONNECTED), a nie wykonać się od razu",
            WorkInfo.State.ENQUEUED,
            infos.single().state,
        )
        assertEquals(
            "TODO 5: drugie syncNow() nie powinno zastępować czekającej pracy (ExistingWorkPolicy.KEEP)",
            first.single().id,
            infos.single().id,
        )
    }
}

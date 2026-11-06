package com.example.catalog.sync

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Sprawdzarka TODO 6: unikalna praca okresowa co 12 h z wymogiem sieci. */
class PeriodicSyncTest : WorkManagerTestBase() {

    @Test
    fun schedulePeriodicTwice_createsSinglePeriodicWork() {
        scheduler.schedulePeriodic()
        scheduler.schedulePeriodic()

        val infos = workManager.getWorkInfosForUniqueWork(SyncScheduler.UNIQUE_PERIODIC).get()
        assertEquals(
            "TODO 6: pod nazwą UNIQUE_PERIODIC powinna być dokładnie jedna praca (enqueueUniquePeriodicWork)",
            1,
            infos.size,
        )
        val info = infos.single()
        val periodicity = info.periodicityInfo
        assertNotNull("TODO 6: praca powinna być okresowa (PeriodicWorkRequest)", periodicity)
        assertEquals("TODO 6: interwał powinien wynosić 12 h", TimeUnit.HOURS.toMillis(12), periodicity!!.repeatIntervalMillis)
        assertEquals(
            "TODO 6: praca okresowa powinna wymagać sieci",
            NetworkType.CONNECTED,
            info.constraints.requiredNetworkType,
        )
    }
}

package com.example.catalog.sync

import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sprawdzarka TODO 5 (część 1/3): wymóg sieci i wykładniczy backoff. */
class SyncRequestTest {

    @Test
    fun syncRequest_requiresNetworkAndUsesExponentialBackoff() {
        val spec = SyncScheduler.buildSyncRequest().workSpec

        assertEquals(
            "TODO 5: żądanie powinno wymagać sieci (NetworkType.CONNECTED)",
            NetworkType.CONNECTED,
            spec.constraints.requiredNetworkType,
        )
        assertEquals("TODO 5: backoff powinien być wykładniczy (EXPONENTIAL)", BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals("TODO 5: początkowe opóźnienie backoffu: 10 s", 10_000L, spec.backoffDelayDuration)
    }
}

package com.example.markstradingscanner

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CockpitFreshnessTest {
    private val now = Instant.parse("2026-08-22T14:00:00Z")

    @Test
    fun currentSnapshotRemainsUsable() {
        val result = snapshotFreshness("2026-08-22T09:59:30-04:00", now)
        assertFalse(result.stale)
        assertTrue(result.label.contains("30 sec"))
    }

    @Test
    fun oldSnapshotIsStale() {
        val result = snapshotFreshness("2026-08-22T09:58:00-04:00", now)
        assertTrue(result.stale)
    }

    @Test
    fun missingOrInvalidTimestampFailsClosed() {
        assertTrue(snapshotFreshness(null, now).stale)
        assertTrue(snapshotFreshness("not-a-time", now).stale)
    }
}

package com.example.markstradingscanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidUpdateManagerTest {
    @Test
    fun comparesSemanticNumericVersions() {
        assertTrue(AndroidUpdateManager.compareVersions("1.1", "1.0") > 0)
        assertTrue(AndroidUpdateManager.compareVersions("1.10", "1.9") > 0)
        assertTrue(AndroidUpdateManager.compareVersions("2.0.1", "2.0") > 0)
        assertEquals(0, AndroidUpdateManager.compareVersions("1.0", "1.0.0"))
    }
}

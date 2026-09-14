package com.example.markstradingscanner

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class V2MonitoringTest {
    private val now=Instant.parse("2026-09-14T11:00:00Z").toEpochMilli()
    private fun snapshot()=JSONObject("""{"generated_at":"2026-09-14T11:00:00Z","last_heartbeat":"2026-09-14T11:00:00Z","health":{"healthy":true}}""")
    @Test fun freshHealthExpiresEvenWithoutAnotherRequest() {
        val data=snapshot()
        assertTrue(v2SnapshotFresh(data,now))
        assertFalse(v2SnapshotFresh(data,now+16000))
    }
    @Test fun missingOrUnhealthyEvidenceNeverGoesGreen() {
        assertFalse(v2SnapshotFresh(JSONObject(),now))
        val data=snapshot();data.getJSONObject("health").put("healthy",false)
        assertFalse(v2SnapshotFresh(data,now))
    }
    @Test fun futureHeartbeatIsUnverified() {
        assertFalse(v2SnapshotFresh(snapshot(),now-1000))
    }
    @Test fun staleRunnerNotRescuedByFreshApiResponse() {
        val data=snapshot();data.put("last_heartbeat","2026-09-14T10:55:00Z")
        assertFalse(v2SnapshotFresh(data,now))
    }
    @Test fun timestampsAgeIndependentlyOfRefresh() {
        assertEquals("10 seconds ago",v2Age(snapshot(),"last_heartbeat",now+10000))
        assertEquals("Unavailable",v2Age(JSONObject(),"last_heartbeat",now))
    }
}

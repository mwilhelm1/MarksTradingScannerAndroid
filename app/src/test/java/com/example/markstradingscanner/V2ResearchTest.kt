package com.example.markstradingscanner

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V2ResearchTest {
    @Test fun routesOnlyToV2WithoutV1OrBrokerRequests() {
        assertEquals("/v2/research",v2ResearchPath(""))
        assertEquals("/v2/research?trading_date=2026-09-11",v2ResearchPath("2026-09-11"))
    }
    @Test fun rejectsInvalidDatesAndQueryInjection() {
        for (date in listOf("2026-02-30","2026-09-11&force_refresh=true","9/11/26")) {
            assertThrows(Exception::class.java) { v2ResearchPath(date) }
        }
    }
    @Test fun missingStateNeverBecomesRunningOrZero() {
        assertEquals("Unavailable",v2Value(JSONObject(),"runtime_status"))
        assertEquals("Unavailable",v2Value(JSONObject("{\"cash_usd\":null}"),"cash_usd"))
        assertTrue(v2Rows(JSONObject(),"paper").isEmpty())
    }
    @Test fun preservesUnavailableReportedStatesAndBoundsRows() {
        val data=JSONObject("{\"runtime_status\":\"STALE_STATUS\",\"order_authority\":\"UNVERIFIED\"}")
        assertEquals("STALE_STATUS",v2Value(data,"runtime_status"))
        assertEquals("UNVERIFIED",v2Value(data,"order_authority"))
        val rows=org.json.JSONArray()
        repeat(40) { rows.put(JSONObject("{\"ticker\":\"TEST\"}")) }
        data.put("paper",JSONObject().put("rows",rows))
        assertEquals(30,v2Rows(data,"paper").size)
    }
    @Test fun timestampsAreEasternAndMissingTimeUnavailable() {
        assertEquals("Sep 11, 2026 07:00:00 ET",v2Time("2026-09-11T11:00:00+00:00"))
        assertEquals("Unavailable",v2Time("bad"))
    }
}

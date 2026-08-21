package com.example.markstradingscanner

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationsParsingTest {
    @Test
    fun parsesAuthoritativeOperationsStatus() {
        val status = ScannerApiClient.parseOperations(JSONObject(PAYLOAD))

        assertEquals("AT_RISK", status.dailyReliability.status)
        assertFalse(status.tradingReady)
        assertTrue(status.operatorActionRequired)
        assertEquals(1, status.brokerPositions)
        assertEquals(0, status.brokerOpenOrders)
        assertTrue(status.reconciliationClean)
        assertEquals("HELD_POSITION_PROTECTION", status.recoveryHolds.single().protectionOwner)
        assertTrue(status.recoveryHolds.single().hardStopProtectionActive)
        assertEquals(125.0, status.postFillRisks.single().riskAmount, 0.0)
    }

    private companion object {
        val PAYLOAD = """
            {
              "generated_at": "2026-08-21T10:00:00-04:00",
              "daily_reliability": {
                "daily_reliability_status": "AT_RISK",
                "at_risk_reasons": ["ACTIVE_POSITION"],
                "failure_reasons": []
              },
              "trading_readiness": {"ready_for_new_entries": false},
              "trading": {"open_positions": 1},
              "broker": {
                "open_orders": 0,
                "synchronization_status": {"synchronized": true}
              },
              "global_status": {"status": "Trading Paused — Safety Gate"},
              "recovery_hold_surveillance": [{
                "trade_id": 2109,
                "ticker": "RDAC",
                "hold_reason": "Review required",
                "responsibility_state": "HELD",
                "protection_owner": "HELD_POSITION_PROTECTION",
                "broker_local_quantity_match": true,
                "hard_stop_protection_active": true,
                "new_entries_blocked": true,
                "requires_operator_action": true
              }],
              "position_responsibilities": [{
                "local_trade_id": 2109,
                "ticker": "RDAC",
                "state": "HELD",
                "protection_owner": "HELD_POSITION_PROTECTION",
                "requires_operator_action": true,
                "blocks_new_entries": true
              }],
              "post_fill_excess_risk": [{
                "trade_id": 2109,
                "ticker": "RDAC",
                "risk_amount": 125.0,
                "configured_limit": 100.0,
                "management_state": "RECOVERY_HOLD"
              }]
            }
        """.trimIndent()
    }
}

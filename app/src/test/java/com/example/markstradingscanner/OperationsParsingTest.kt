package com.example.markstradingscanner

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationsParsingTest {
    @Test
    fun parsesCockpitContractWithoutRecalculatingBackendVerdicts() {
        val status = ScannerApiClient.parseOperations(JSONObject(FULL_PAYLOAD))

        assertEquals("FAILED", status.dailyReliability.status)
        assertEquals("PASS", status.cleanTradingDay?.status)
        assertTrue(status.cleanTradingDay?.completed == true)
        assertFalse(status.cleanTradingDay?.historicalStreakAvailable ?: true)
        assertEquals(3, status.todaySummary?.tradeAttempts)
        assertEquals(2, status.todaySummary?.completedTrades)
        assertEquals(25.0, status.todaySummary?.realizedPl ?: 0.0, 0.0)
        assertEquals("SUGP", status.latestTrade?.ticker)
        assertEquals("TARGET", status.latestTrade?.exitReason)
        assertEquals("CRITICAL", status.actionableIncidents.single().severity)
        assertTrue(status.actionableIncidents.single().operatorActionRequired == true)
        assertEquals("UNRESOLVED_UNKNOWN_EXECUTION_INTENT", status.actionableIncidents.single().code)
        assertEquals("POTENTIAL", status.actionableIncidents.single().tradingImpact)
        assertEquals("Healthy", status.componentHealth.first().status)
        assertEquals("Running", status.componentHealth.first().detail)
        assertEquals(123, status.componentHealth.first().pid)
        assertEquals(123, status.componentHealth.first().instances.single().pid)
        assertEquals("2026-08-22T09:00:00-04:00", status.componentHealth.first().lastSuccessfulUpdate)
        assertTrue(status.tradingReady == true)
        assertTrue(status.operatorActionRequired == true)
        assertEquals(listOf("UNRESOLVED_UNKNOWN_INTENT"), status.readinessReasonCodes)
        assertTrue(status.reconciliationClean == true)
        assertEquals("SUGP", status.reconciliationDifferences.single().ticker)
        assertEquals(10.0, status.reconciliationDifferences.single().localQuantity ?: 0.0, 0.0)
        assertEquals(8.0, status.reconciliationDifferences.single().brokerQuantity ?: 0.0, 0.0)
        assertEquals(0, status.unresolvedUnknownIntents)
        assertEquals(0, status.unresolvedAmbiguousIntents)
        assertEquals("POST_CLOSE", status.eodStatus?.phase)
    }

    @Test
    fun missingNewFieldsRemainUnavailableAndDoNotFabricateStreak() {
        val status = ScannerApiClient.parseOperations(JSONObject(LEGACY_PAYLOAD))

        assertNull(status.cleanTradingDay)
        assertNull(status.todaySummary)
        assertNull(status.latestTrade)
        assertTrue(status.actionableIncidents.isEmpty())
        assertTrue(status.componentHealth.isEmpty())
        assertNull(status.unresolvedUnknownIntents)
        assertNull(status.unresolvedAmbiguousIntents)
        assertNull(status.eodStatus)
        assertNull(status.operatorActionRequired)
        assertTrue(status.readinessReasonCodes.isEmpty())
        assertTrue(status.reconciliationDifferences.isEmpty())
    }

    @Test
    fun emptyActionableIncidentCollectionRemainsEmpty() {
        val payload = JSONObject(FULL_PAYLOAD).put(
            "actionable_incidents",
            JSONArray(),
        ).put("global_status", JSONObject().put("status", "Trading Normally"))
            .put("operator_action_required", false)
        val status = ScannerApiClient.parseOperations(payload)
        assertTrue(status.actionableIncidents.isEmpty())
        assertFalse(status.operatorActionRequired ?: true)
    }

    @Test
    fun operatorActionUsesBackendVerdictWithoutLocalInference() {
        val payload = JSONObject(FULL_PAYLOAD).put(
            "operator_action_required",
            false,
        )
        val status = ScannerApiClient.parseOperations(payload)

        assertTrue(status.actionableIncidents.single().operatorActionRequired == true)
        assertFalse(status.operatorActionRequired ?: true)
    }

    @Test
    fun unavailableOperationsResultContainsNoAuthoritativeStatus() {
        val result = OperationsResult(unavailableReason = "HTTP 503")
        assertNull(result.status)
        assertEquals("HTTP 503", result.unavailableReason)
    }

    private companion object {
        val FULL_PAYLOAD = """
            {
              "generated_at": "2026-08-22T10:00:00-04:00",
              "overall_status": "NORMAL",
              "daily_reliability": {
                "daily_reliability_status": "FAILED",
                "at_risk_reasons": [],
                "failure_reasons": ["OPERATIONS_CENTER_OFFLINE"],
                "eod_status": {"phase": "POST_CLOSE", "flatness": "FLAT", "verification": "READY"}
              },
              "clean_trading_day": {
                "status": "PASS", "completed": true, "reasons": [],
                "trading_impact": "NONE", "manual_intervention_required": false,
                "historical_streak_available": false
              },
              "today_summary": {
                "trade_attempts": 3, "filled_trades": 2, "entered_trades": 2,
                "completed_trades": 2, "winners": 1, "losers": 1,
                "realized_pl": 25.0, "unrealized_pl": 0.0,
                "open_positions": 0, "broker_open_orders": 0
              },
              "latest_trade": {
                "trade_id": 2201, "ticker": "SUGP",
                "entry_time": "2026-08-22T09:10:00-04:00",
                "exit_time": "2026-08-22T09:30:00-04:00",
                "entry_price": 3.82, "exit_price": 4.10,
                "realized_pl": 167.72, "exit_reason": "TARGET", "status": "CLOSED"
              },
              "actionable_incidents": [{
                "code": "UNRESOLVED_UNKNOWN_EXECUTION_INTENT", "severity": "CRITICAL",
                "summary": "Execution outcome is unknown and must be resolved.",
                "operator_action_required": true, "trading_impact": "POTENTIAL"
              }],
              "operator_action_required": true,
              "system_health": [{
                "component": "Scanner", "status": "Healthy", "detail": "Running",
                "pid": 123, "instances": [{"pid": 123, "started_at": "2026-08-22T09:00:00-04:00"}],
                "last_successful_update": "2026-08-22T09:00:00-04:00"
              }],
              "trading_readiness": {
                "ready_for_new_entries": true,
                "unresolved_unknown_intents": 0,
                "unresolved_ambiguous_intents": 0,
                "reason_codes": ["UNRESOLVED_UNKNOWN_INTENT"]
              },
              "trading": {"open_positions": 0},
              "broker": {"open_orders": 0, "synchronization_status": {
                "synchronized": true,
                "differences": [{"symbol": "SUGP", "local_quantity": 10, "broker_quantity": 8}]
              }},
              "global_status": {"status": "Operator Action Required"},
              "recovery_hold_surveillance": [],
              "position_responsibilities": [],
              "post_fill_excess_risk": []
            }
        """.trimIndent()

        val LEGACY_PAYLOAD = """
            {
              "generated_at": null,
              "daily_reliability": {
                "daily_reliability_status": "AT_RISK",
                "at_risk_reasons": [], "failure_reasons": []
              },
              "trading_readiness": {"ready_for_new_entries": false},
              "trading": {"open_positions": 0},
              "broker": {"open_orders": 0, "synchronization_status": {"synchronized": true}},
              "global_status": {"status": "Trading Paused — Safety Gate"},
              "recovery_hold_surveillance": [],
              "position_responsibilities": [],
              "post_fill_excess_risk": []
            }
        """.trimIndent()
    }
}

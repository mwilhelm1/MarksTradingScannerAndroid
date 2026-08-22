package com.example.markstradingscanner

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceParsingTest {
    @Test
    fun parsesEvidenceContractWithoutLocalReadinessPromotion() {
        listOf("COLLECTING", "EARLY", "READY_FOR_REVIEW").forEach { state ->
            val parsed = ScannerApiClient.parseEvidence(
                JSONObject(FULL_PAYLOAD.replace("STATE_VALUE", state))
            )
            assertEquals(state, parsed.status.state)
        }

        val evidence = ScannerApiClient.parseEvidence(
            JSONObject(FULL_PAYLOAD.replace("STATE_VALUE", "COLLECTING"))
        )
        assertEquals("COLLECTING", evidence.status.state)
        assertEquals("premarket", evidence.sessions.first().group)
        assertEquals("UNCLASSIFIED", evidence.sessions.last().group)
        assertEquals(37.5, evidence.tradeBehavior.captureEfficiency?.percent ?: 0.0, 0.0)
        assertEquals(138, evidence.tradeBehavior.captureEfficiency?.sampleSize)
        assertEquals("MANUAL_INCIDENT_FLATTEN", evidence.exits.single().group)
        assertEquals("FIRST", evidence.reEntries.buckets.first().group)
        assertFalse(evidence.reEntries.sameSessionAvailable ?: true)
        assertEquals("momentum-setup-v2", evidence.currentStrategy.detectorVersion)
        assertEquals("forward-outcome-v1", evidence.currentStrategy.outcomeCalculationVersion)
        assertEquals(14848, evidence.strategyIntelligence.correctedObservations)
        assertFalse(evidence.strategyIntelligence.legacyIncludedInPerformance ?: true)
        assertFalse(evidence.cleanTradingDays.available ?: true)
        assertNull(evidence.cleanTradingDays.count)
    }

    @Test
    fun missingFieldsRemainUnavailableRatherThanReady() {
        val evidence = ScannerApiClient.parseEvidence(JSONObject("{}"))
        assertNull(evidence.status.state)
        assertNull(evidence.overview.eligibleTrades)
        assertTrue(evidence.sessions.isEmpty())
        assertTrue(evidence.exits.isEmpty())
        assertTrue(evidence.strategyIntelligence.setups.isEmpty())
        assertNull(evidence.cleanTradingDays.available)
    }

    @Test
    fun unavailableResultContainsNoSnapshot() {
        val result = EvidenceResult(unavailableReason = "HTTP 503")
        assertNull(result.snapshot)
        assertEquals("HTTP 503", result.unavailableReason)
    }

    private companion object {
        val FULL_PAYLOAD = """
            {
              "generated_at":"2026-08-22T10:00:00-04:00",
              "evidence_status":{
                "state":"STATE_VALUE","authoritative_verdict":"COLLECTING",
                "independent_events":1093,"trading_days":10,
                "failed_gates":["trading_days"],"warnings":[],
                "semantics":"Human review readiness only; not authorization for strategy changes."
              },
              "overview":{
                "total_closed_trades":223,"eligible_trades":211,"excluded_trades":12,
                "realized_pl":25.0,"win_rate_percent":50.0,"average_winner":4.0,
                "average_loser":-2.0,"profit_factor":1.5,"average_hold_minutes":15.0
              },
              "sessions":[
                {"group":"premarket","trade_count":11,"realized_pl":10.0},
                {"group":"UNCLASSIFIED","trade_count":162,"realized_pl":null}
              ],
              "trade_behavior":{
                "average_hold_minutes":15.0,"average_mfe_percent":8.0,
                "average_mae_percent":-3.0,
                "capture_efficiency":{"label":"Capture Efficiency","percent":37.5,
                  "sample_size":138,"semantics":"Backend semantics"}
              },
              "exits":[{"group":"MANUAL_INCIDENT_FLATTEN","trade_count":2}],
              "re_entries":{"scope":"SAME_DAY","buckets":[{"group":"FIRST","trade_count":100}],
                "same_session":{"available":false,"reason":"Labels incomplete"}},
              "strategy_intelligence":{
                "cohort":{"name":"CURRENT_VERSIONED_DETECTOR_OBSERVATIONS",
                  "legacy_observations":38180,"corrected_observations":14848,
                  "corrected_outcomes":13624,"legacy_included_in_performance":false},
                "setups":[{"setup_type":"HIGH_OF_DAY_BREAKOUT","independent_events":280,
                  "controls":8811,"outcome_count":200,"maturity_status":"IMMATURE",
                  "failed_maturity_gates":["trading_days"]}],"warnings":[]
              },
              "current_strategy":{"trade_plan_versions":["1"],
                "detector_version":"momentum-setup-v2",
                "outcome_calculation_version":"forward-outcome-v1"},
              "clean_trading_days":{"available":false,"count":null,
                "reason":"Completed clean-day history is not yet persisted."},
              "data_quality":{"included_trades":211,"excluded_trades":12,
                "exclusion_reasons":{"INCONSISTENT_LIFECYCLE":12},
                "metric_sample_sizes":{"pnl":207},"unclassified_session_count":162,
                "cohort_labels":{"completed_trades":"ELIGIBLE_CLOSED_PAPER_TRADES"},
                "warnings":[]}
            }
        """.trimIndent()
    }
}

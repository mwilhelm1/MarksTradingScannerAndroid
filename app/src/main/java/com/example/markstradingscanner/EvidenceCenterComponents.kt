package com.example.markstradingscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val incidentExitReasons = setOf(
    "MANUAL_INCIDENT_FLATTEN",
    "POST_EOD_INCIDENT_FLATTEN",
    "BROKER_POSITION_CLOSED",
)

@Composable
fun EvidenceCenterHost(onCockpit: () -> Unit) {
    var result by remember { mutableStateOf(EvidenceResult()) }
    var refreshRequest by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshRequest) {
        loading = result.snapshot == null
        refreshing = true
        val loaded = ScannerApiClient.loadEvidence(forceRefresh = refreshRequest > 0)
        result = if (loaded.snapshot == null && result.snapshot != null) {
            result.copy(unavailableReason = loaded.unavailableReason)
        } else {
            loaded
        }
        loading = false
        refreshing = false
    }

    EvidenceCenterScreen(
        result = result,
        loading = loading,
        refreshing = refreshing,
        onCockpit = onCockpit,
        onRefresh = { refreshRequest += 1 },
    )
}

@Composable
fun TopLevelSwitch(
    selected: String,
    onCockpit: () -> Unit,
    onEvidence: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selected == "COCKPIT") {
            Button(onClick = onCockpit, modifier = Modifier.weight(1f)) {
                Text("Cockpit")
            }
        } else {
            OutlinedButton(onClick = onCockpit, modifier = Modifier.weight(1f)) {
                Text("Cockpit")
            }
        }
        if (selected == "EVIDENCE") {
            Button(onClick = onEvidence, modifier = Modifier.weight(1f)) {
                Text("Evidence")
            }
        } else {
            OutlinedButton(onClick = onEvidence, modifier = Modifier.weight(1f)) {
                Text("Evidence")
            }
        }
    }
}

@Composable
fun EvidenceCenterScreen(
    result: EvidenceResult,
    loading: Boolean,
    refreshing: Boolean,
    onCockpit: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Evidence Center",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TopLevelSwitch("EVIDENCE", onCockpit, {})
                }
            }

            val snapshot = result.snapshot
            if (snapshot == null) {
                item {
                    SectionCard(if (loading) "Loading evidence…" else "EVIDENCE UNAVAILABLE") {
                        Text(
                            result.unavailableReason
                                ?: "Evidence has not been loaded. No readiness state is inferred.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                if (result.unavailableReason != null) {
                    item {
                        SectionCard("EVIDENCE STALE") {
                            Text(result.unavailableReason)
                            Text("Showing the last successful snapshot.")
                        }
                    }
                }
                item { EvidenceStatusCard(snapshot) }
                item { EvidenceOverviewCard(snapshot.overview) }
                item { EvidenceGroupsCard("Sessions", snapshot.sessions) }
                item { TradeBehaviorCard(snapshot.tradeBehavior) }
                item { EvidenceGroupsCard("Exits", snapshot.exits, incident = true) }
                item {
                    EvidenceGroupsCard(
                        "Same-day re-entry analysis",
                        snapshot.reEntries.buckets,
                    )
                }
                item {
                    SectionCard("Same-session analysis") {
                        DashboardMetric(
                            "Availability",
                            if (snapshot.reEntries.sameSessionAvailable == true) {
                                "Available"
                            } else {
                                "Unavailable"
                            },
                        )
                        snapshot.reEntries.sameSessionReason?.let { Text(it) }
                    }
                }
                item { StrategyIntelligenceCard(snapshot) }
                item { DataQualityCard(snapshot.dataQuality) }
                item {
                    SectionCard("Clean Trading Day History") {
                        DashboardMetric(
                            "History",
                            if (snapshot.cleanTradingDays.available == true) {
                                snapshot.cleanTradingDays.count?.toString() ?: "Unavailable"
                            } else {
                                "Not yet persisted"
                            },
                        )
                        snapshot.cleanTradingDays.reason?.let { Text(it) }
                    }
                }
                item {
                    Button(
                        onClick = onRefresh,
                        enabled = !refreshing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (refreshing) "Refreshing evidence…" else "Refresh Evidence")
                    }
                }
            }
            item {
                Text(
                    "Evidence refreshes only when opened or manually requested.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun EvidenceStatusCard(snapshot: EvidenceSnapshot) {
    SectionCard("EVIDENCE STATUS") {
        DashboardMetric("Status", displayText(snapshot.status.state ?: "UNAVAILABLE"))
        DashboardMetric(
            "Authoritative verdict",
            displayText(snapshot.status.authoritativeVerdict ?: "UNAVAILABLE"),
        )
        DashboardMetric("Valid trades", value(snapshot.overview.eligibleTrades))
        DashboardMetric("Trading days", value(snapshot.status.tradingDays))
        DashboardMetric("Independent events", value(snapshot.status.independentEvents))
        DashboardMetric("Failed gates", snapshot.status.failedGates.joinToString().ifBlank { "None" })
        Text(
            snapshot.status.semantics
                ?: "Human review readiness only — not authorization for strategy changes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DashboardMetric("Generated", snapshot.generatedAt ?: "Unavailable")
    }
}

@Composable
private fun EvidenceOverviewCard(item: EvidencePerformance) {
    SectionCard("Overview") {
        DashboardMetric("Completed trades", value(item.totalClosedTrades))
        DashboardMetric("Valid / eligible", value(item.eligibleTrades))
        DashboardMetric("Excluded", value(item.excludedTrades))
        DashboardMetric("Realized P/L", money(item.realizedPl))
        DashboardMetric("Win rate", percent(item.winRatePercent))
        DashboardMetric("Average winner", percent(item.averageWinner))
        DashboardMetric("Average loser", percent(item.averageLoser))
        DashboardMetric("Profit factor", item.profitFactor ?: "Unavailable")
        DashboardMetric("Average hold", minutes(item.averageHoldMinutes))
    }
}

@Composable
private fun TradeBehaviorCard(item: EvidenceTradeBehavior) {
    SectionCard("Trade Behavior") {
        DashboardMetric("Average hold", minutes(item.averageHoldMinutes))
        DashboardMetric("Average MFE", percent(item.averageMfePercent))
        DashboardMetric("Average MAE", percent(item.averageMaePercent))
        item.captureEfficiency?.let {
            DashboardMetric(it.label ?: "Capture Efficiency", percent(it.percent))
            DashboardMetric("Capture sample", value(it.sampleSize))
            it.semantics?.let { semantics -> Text(semantics) }
        }
    }
}

@Composable
private fun EvidenceGroupsCard(
    title: String,
    groups: List<EvidencePerformance>,
    incident: Boolean = false,
) {
    SectionCard(title) {
        if (groups.isEmpty()) Text("No evidence groups available.")
        groups.forEach { group ->
            EvidenceGroup(group, incident && group.group in incidentExitReasons)
        }
    }
}

@Composable
private fun EvidenceGroup(item: EvidencePerformance, incident: Boolean) {
    var expanded by remember(item.group) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(displayText(item.group ?: "UNCLASSIFIED"), fontWeight = FontWeight.Bold)
                Text(if (incident) "INCIDENT · ${value(item.tradeCount)}" else value(item.tradeCount))
            }
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide details" else "Show details")
            }
            if (expanded) {
                DashboardMetric("P/L", money(item.realizedPl))
                DashboardMetric("Win rate", percent(item.winRatePercent))
                DashboardMetric("Average return", percent(item.averageReturnPercent))
                DashboardMetric("MFE", percent(item.averageMfePercent))
                DashboardMetric("MAE", percent(item.averageMaePercent))
                DashboardMetric("Hold", minutes(item.averageHoldMinutes))
            }
        }
    }
}

@Composable
private fun StrategyIntelligenceCard(snapshot: EvidenceSnapshot) {
    val item = snapshot.strategyIntelligence
    SectionCard("SHADOW STRATEGY INTELLIGENCE") {
        Text("Separate from actual completed-trade performance.")
        DashboardMetric("Detector", snapshot.currentStrategy.detectorVersion ?: "Unavailable")
        DashboardMetric(
            "Outcome version",
            snapshot.currentStrategy.outcomeCalculationVersion ?: "Unavailable",
        )
        DashboardMetric("Corrected observations", value(item.correctedObservations))
        DashboardMetric("Corrected outcomes", value(item.correctedOutcomes))
        DashboardMetric("Legacy collection only", value(item.legacyObservations))
        DashboardMetric(
            "Legacy in performance",
            if (item.legacyIncludedInPerformance == true) "YES" else "NO",
        )
        item.setups.forEach { setup ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(displayText(setup.setupType), fontWeight = FontWeight.Bold)
                    DashboardMetric("Maturity", displayText(setup.maturityStatus ?: "UNKNOWN"))
                    DashboardMetric("Independent events", value(setup.independentEvents))
                    DashboardMetric("Controls", value(setup.controls))
                    DashboardMetric("Outcomes", value(setup.outcomeCount))
                    DashboardMetric(
                        "Failed gates",
                        setup.failedMaturityGates.joinToString().ifBlank { "None" },
                    )
                }
            }
        }
    }
}

@Composable
private fun DataQualityCard(item: EvidenceDataQuality) {
    SectionCard("Data Quality") {
        DashboardMetric("Included trades", value(item.includedTrades))
        DashboardMetric("Excluded trades", value(item.excludedTrades))
        DashboardMetric("Unclassified sessions", value(item.unclassifiedSessionCount))
        DashboardMetric(
            "Exclusion reasons",
            item.exclusionReasons.entries.joinToString { "${displayText(it.key)}: ${it.value}" }
                .ifBlank { "None" },
        )
        DashboardMetric(
            "Metric samples",
            item.metricSampleSizes.entries.joinToString { "${displayText(it.key)}: ${it.value}" }
                .ifBlank { "None" },
        )
        DashboardMetric("Warnings", item.warnings.joinToString().ifBlank { "None" })
    }
}

private fun value(value: Int?): String = value?.toString() ?: "Unavailable"
private fun money(value: Double?): String = value?.let(::formatSignedCurrency) ?: "Unavailable"
private fun percent(value: Double?): String = value?.let(::formatPercent) ?: "Unavailable"
private fun minutes(value: Double?): String = value?.let { "${it.format(1)} min" } ?: "Unavailable"

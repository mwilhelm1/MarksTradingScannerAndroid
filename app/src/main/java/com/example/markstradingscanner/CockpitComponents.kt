package com.example.markstradingscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

private const val OPERATIONS_STALE_AFTER_SECONDS = 90L

data class SnapshotFreshness(
    val label: String,
    val stale: Boolean,
)

fun snapshotFreshness(
    generatedAt: String?,
    now: Instant = Instant.now(),
    staleAfterSeconds: Long = OPERATIONS_STALE_AFTER_SECONDS,
): SnapshotFreshness {
    if (generatedAt.isNullOrBlank()) {
        return SnapshotFreshness("Snapshot unavailable", true)
    }
    val generated = runCatching {
        OffsetDateTime.parse(generatedAt).toInstant()
    }.getOrNull() ?: return SnapshotFreshness("Snapshot time unknown", true)
    val ageSeconds = Duration.between(generated, now).seconds.coerceAtLeast(0)
    return if (ageSeconds > staleAfterSeconds) {
        SnapshotFreshness("STALE • ${ageSeconds / 60} min old", true)
    } else {
        SnapshotFreshness("Current • $ageSeconds sec old", false)
    }
}

@Composable
fun CockpitHeader(
    operations: OperationsResult,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val freshness = snapshotFreshness(operations.status?.generatedAt)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Mark's Trading Scanner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = freshness.label,
            color = if (freshness.stale) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        operations.status?.generatedAt?.let {
            Text(
                text = "Generated: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onRefresh,
            enabled = !refreshing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (refreshing) "Refreshing…" else "Refresh")
        }
    }
}

@Composable
fun OperationsCockpit(result: OperationsResult) {
    val freshness = snapshotFreshness(result.status?.generatedAt)
    val operations = result.status.takeUnless { freshness.stale }

    if (operations == null) {
        SectionCard("Operations Status") {
            Text(
                "OPERATIONS UNAVAILABLE / STALE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(result.unavailableReason ?: freshness.label)
            Text("READY, PASS, and safety states are not inferred.")
        }
    }

    PrimaryStatusCard(operations)
    SystemHealthCard(operations)
    TodayCard(operations?.todaySummary)
    TradingReliabilityCard(operations)
    SafetyCard(operations)
    LatestTradeCard(operations?.latestTrade)
    AttentionCard(
        incidents = operations?.actionableIncidents,
        responsibilities = operations?.responsibilities,
    )
}

@Composable
private fun PrimaryStatusCard(operations: OperationsStatus?) {
    SectionCard("Primary Status") {
        DashboardMetric(
            "Trading",
            when (operations?.tradingReady) {
                true -> "READY"
                false -> "BLOCKED"
                null -> "UNKNOWN"
            },
        )
        DashboardMetric(
            "Operator Action",
            when (operations?.operatorActionRequired) {
                true -> "REQUIRED"
                false -> "NONE"
                null -> "UNKNOWN"
            },
        )
        DashboardMetric("Platform", statusText(operations?.overallStatus))
        if (operations?.tradingReady == false) {
            operations.readinessReasonCodes.forEach {
                Text("- ${displayText(it)}")
            }
        }
    }
}

@Composable
private fun SystemHealthCard(operations: OperationsStatus?) {
    val components = operations?.componentHealth.orEmpty().associateBy {
        it.component.lowercase()
    }
    SectionCard("System Health") {
        listOf("Scanner", "Scheduler", "Paper Monitor", "Broker", "Dashboard")
            .forEach { name ->
                val component = components[name.lowercase()]
                val label = if (name == "Dashboard") "Operations Center" else name
                DashboardMetric(label, statusText(component?.status))
                component?.detail?.let { Text(it) }
                component?.pid?.let { DashboardMetric("$label PID", it.toString()) }
                if (!component?.instances.isNullOrEmpty()) {
                    DashboardMetric(
                        "$label Instances",
                        component!!.instances.joinToString { instance ->
                            instance.pid?.toString() ?: "UNKNOWN"
                        },
                    )
                }
                component?.lastSuccessfulUpdate?.let {
                    DashboardMetric("$label Last Success", it)
                }
            }
    }
}

@Composable
private fun TodayCard(summary: TodaySummary?) {
    SectionCard("Today") {
        DashboardMetric("Trades", summary?.tradeAttempts?.toString() ?: "UNKNOWN")
        DashboardMetric(
            "Completed",
            summary?.completedTrades?.toString() ?: "UNKNOWN",
        )
        DashboardMetric(
            "Realized P/L",
            summary?.realizedPl?.let(::formatSignedCurrency) ?: "UNKNOWN",
        )
        DashboardMetric(
            "Open Positions",
            summary?.openPositions?.toString() ?: "UNKNOWN",
        )
        DashboardMetric(
            "Open Orders",
            summary?.brokerOpenOrders?.toString() ?: "UNKNOWN",
        )
        DashboardMetric(
            "Winners / Losers",
            if (summary?.winners != null && summary.losers != null) {
                "${summary.winners} / ${summary.losers}"
            } else {
                "UNKNOWN"
            },
        )
    }
}

@Composable
private fun TradingReliabilityCard(operations: OperationsStatus?) {
    val clean = operations?.cleanTradingDay
    SectionCard("Trading Reliability") {
        DashboardMetric("Clean Trading Day", statusText(clean?.status))
        DashboardMetric(
            "Platform Reliability",
            statusText(operations?.dailyReliability?.status),
        )
        if (clean?.historicalStreakAvailable == false) {
            DashboardMetric("Streak", "Not yet tracked")
        }
        clean?.reasons.orEmpty().forEach { Text("• ${displayText(it)}") }
    }
}

@Composable
private fun SafetyCard(operations: OperationsStatus?) {
    SectionCard("Safety") {
        DashboardMetric(
            "Reconciliation",
            when (operations?.reconciliationClean) {
                true -> "CLEAN"
                false -> "BLOCKED"
                null -> "UNKNOWN"
            },
        )
        operations?.reconciliationDifferences.orEmpty().forEach { difference ->
            Text("${difference.ticker}: local ${difference.localQuantity ?: "UNKNOWN"} / " +
                "broker ${difference.brokerQuantity ?: "UNKNOWN"}")
        }
        DashboardMetric(
            "UNKNOWN Intents",
            operations?.unresolvedUnknownIntents?.toString() ?: "UNKNOWN",
        )
        DashboardMetric(
            "Ambiguous Intents",
            operations?.unresolvedAmbiguousIntents?.toString() ?: "UNKNOWN",
        )
        val eod = operations?.eodStatus
        DashboardMetric("EOD Phase", statusText(eod?.phase))
        DashboardMetric("EOD Flatness", statusText(eod?.flatness))
        DashboardMetric("EOD Verification", statusText(eod?.verification))
    }
}

@Composable
private fun LatestTradeCard(trade: LatestTradeSummary?) {
    SectionCard("Latest Trade") {
        if (trade == null) {
            Text("No trade today")
            return@SectionCard
        }
        DashboardMetric("Ticker", trade.ticker ?: "UNKNOWN")
        DashboardMetric(
            "Entry → Exit",
            "${trade.entryPrice?.let(::formatCurrency) ?: "—"} → " +
                (trade.exitPrice?.let(::formatCurrency) ?: "—"),
        )
        DashboardMetric(
            "Realized P/L",
            trade.realizedPl?.let(::formatSignedCurrency) ?: "UNAVAILABLE",
        )
        DashboardMetric("Exit Reason", statusText(trade.exitReason))
        DashboardMetric("Final Status", statusText(trade.status))
        trade.entryTime?.let { DashboardMetric("Entry Time", it) }
        trade.exitTime?.let { DashboardMetric("Exit Time", it) }
    }
}

@Composable
private fun AttentionCard(
    incidents: List<ActionableIncidentSummary>?,
    responsibilities: List<PositionResponsibilitySummary>?,
) {
    SectionCard("Attention") {
        when {
            incidents == null -> Text("UNKNOWN — Operations data unavailable")
            incidents.isEmpty() -> Text("No action required")
            else -> incidents.forEach { incident ->
                Text(
                    text = statusText(incident.severity),
                    fontWeight = FontWeight.Bold,
                    color = if (incident.severity.equals("CRITICAL", true)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(incident.summary ?: statusText(incident.code))
                DashboardMetric("Incident Code", statusText(incident.code))
                DashboardMetric(
                    "Trading Impact",
                    statusText(incident.tradingImpact),
                )
                DashboardMetric(
                    "Operator Action",
                    when (incident.operatorActionRequired) {
                        true -> "REQUIRED"
                        false -> "AWARENESS"
                        null -> "UNKNOWN"
                    },
                )
            }
        }
        responsibilities.orEmpty()
            .filter { it.requiresOperatorAction }
            .forEach { responsibility ->
                Text(
                    "${responsibility.ticker} responsibility evidence",
                    fontWeight = FontWeight.Bold,
                )
                DashboardMetric("State", statusText(responsibility.state))
                DashboardMetric(
                    "Protection Owner",
                    statusText(responsibility.protectionOwner),
                )
                DashboardMetric(
                    "Evidence",
                    statusText(responsibility.evidenceConfidence),
                )
                DashboardMetric(
                    "Broker / Local Quantity",
                    "${responsibility.brokerQuantity ?: "UNKNOWN"} / " +
                        "${responsibility.localQuantity ?: "UNKNOWN"}",
                )
                responsibility.holdReason?.let { Text(it) }
                responsibility.reasonCodes.forEach {
                    Text("â€¢ ${displayText(it)}")
                }
            }
    }
}

private fun statusText(value: String?): String =
    value?.takeIf { it.isNotBlank() }?.uppercase() ?: "UNKNOWN"

package com.example.markstradingscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.example.markstradingscanner.ui.theme.MarksTradingScannerTheme
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MarksTradingScannerTheme {
                TradingScannerApp()
            }
        }
    }
}

@Composable
private fun TradingScannerApp() {
    var selectedPosition by remember {
        mutableStateOf<PositionSummary?>(null)
    }

    val position = selectedPosition

    if (position == null) {
        HomeScreen(
            onPositionSelected = {
                selectedPosition = it
            },
        )
    } else {
        BackHandler {
            selectedPosition = null
        }

        TradeDetailScreen(
            position = position,
            onBack = {
                selectedPosition = null
            },
        )
    }
}


@Composable
private fun HomeScreen(
    onPositionSelected: (PositionSummary) -> Unit,
) {
    var dashboard by remember {
        mutableStateOf<DashboardData?>(null)
    }
    var loading by remember {
        mutableStateOf(true)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var refreshRequest by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(refreshRequest) {
        loading = true
        error = null

        try {
            dashboard = ScannerApiClient.loadDashboard()
        } catch (exception: Exception) {
            error = exception.message ?: "Unknown connection error"
        } finally {
            loading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
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
                ) {
                    Text(
                        text = "Mark's Trading Scanner",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "Mobile Trading Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                loading -> {
                    item {
                        LoadingCard()
                    }
                }

                error != null -> {
                    item {
                        ErrorCard(
                            message = error ?: "Unknown error",
                            onRetry = {
                                refreshRequest += 1
                            },
                        )
                    }
                }

                dashboard != null -> {
                    val data = dashboard!!

                    item {
                        SystemCard(data.system)
                    }

                    item {
                        AccountCard(data.account)
                    }

                    item {
                        PerformanceCard(data.performance)
                    }

                    item {
                        PositionsCard(
                            positions = data.positions,
                            onPositionSelected = onPositionSelected,
                        )
                    }

                    item {
                        OrdersCard(data.orders)
                    }

                    item {
                        ScannerResultsCard(data.scannerResults)
                    }

                    item {
                        RecentTradesCard(data.trades)
                    }

                    item {
                        Button(
                            onClick = {
                                refreshRequest += 1
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Refresh All Data")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Secure connection through Tailscale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun TradeDetailScreen(
    position: PositionSummary,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
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
                        text = position.symbol,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "Trade Detail",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Back to Dashboard")
                    }
                }
            }

            item {
                SectionCard(
                    title = "Live Position",
                ) {
                    DashboardMetric(
                        "Side",
                        displayText(position.side),
                    )
                    DashboardMetric(
                        "Quantity",
                        formatQuantity(position.quantity),
                    )
                    DashboardMetric(
                        "Entry",
                        formatCurrency(position.entryPrice),
                    )
                    DashboardMetric(
                        "Current",
                        formatCurrency(position.currentPrice),
                    )
                    DashboardMetric(
                        "Market value",
                        formatCurrency(position.marketValue),
                    )
                    DashboardMetric(
                        "Unrealized P/L",
                        formatSignedCurrency(position.unrealizedPl),
                    )
                    DashboardMetric(
                        "Change today",
                        formatPercent(position.changeToday * 100),
                    )
                }
            }

            item {
                SectionCard(
                    title = "Trade Plan",
                ) {
                    position.score?.let {
                        DashboardMetric(
                            "AI score",
                            it.format(1),
                        )
                    }

                    position.stopPrice?.let {
                        DashboardMetric(
                            "Stop",
                            formatCurrency(it),
                        )
                    }

                    position.targetPrice?.let {
                        DashboardMetric(
                            "Target",
                            formatCurrency(it),
                        )
                    }

                    position.riskAmount?.let {
                        DashboardMetric(
                            "Planned risk",
                            formatCurrency(it),
                        )
                    }

                    position.riskPerShare?.let {
                        DashboardMetric(
                            "Risk per share",
                            formatCurrency(it),
                        )
                    }

                    position.rewardToRisk?.let {
                        DashboardMetric(
                            "Reward / Risk",
                            it.format(2),
                        )
                    }

                    position.plannedStopDistancePercent?.let {
                        DashboardMetric(
                            "Stop distance",
                            formatPercent(it),
                        )
                    }

                    position.stopMethod?.let {
                        DashboardMetric(
                            "Stop method",
                            displayText(it),
                        )
                    }

                    position.targetMethod?.let {
                        DashboardMetric(
                            "Target method",
                            displayText(it),
                        )
                    }

                    position.tradePlanVersion?.let {
                        DashboardMetric(
                            "Plan version",
                            it,
                        )
                    }

                    if (
                        position.stopMethod == null
                        && position.targetMethod == null
                        && position.tradePlanVersion == null
                    ) {
                        Text(
                            text = (
                                "Planning-method details are unavailable "
                                + "for this older position."
                            ),
                            color = (
                                MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "Broker Record",
                ) {
                    position.localTradeId?.let {
                        DashboardMetric(
                            "Local trade ID",
                            it.toString(),
                        )
                    }

                    position.brokerStatus?.let {
                        DashboardMetric(
                            "Broker status",
                            displayText(it),
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Secure connection through Tailscale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}


@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()

            Column {
                Text(
                    text = "Loading dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text("Connecting to scanner and broker...")
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Connection failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(message)

            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun SystemCard(system: SystemStatus) {
    SectionCard(
        title = "System Status",
    ) {
        DashboardMetric(
            "Scanner API",
            if (system.status.equals("ok", true)) {
                "ONLINE"
            } else {
                "OFFLINE"
            },
        )
        DashboardMetric(
            "Broker",
            if (system.brokerConnected) {
                "CONNECTED"
            } else {
                "DISCONNECTED"
            },
        )
        DashboardMetric(
            "Broker health",
            if (system.brokerHealthy) "HEALTHY" else "UNHEALTHY",
        )
        DashboardMetric(
            "Market session",
            displayText(system.marketSession),
        )
        DashboardMetric(
            "Execution mode",
            displayText(system.executionMode),
        )
        DashboardMetric(
            "Latest scan",
            system.latestScanId?.let { "#$it" } ?: "Unavailable",
        )
        DashboardMetric(
            "Scan status",
            system.latestScanStatus?.let(::displayText)
                ?: "Unavailable",
        )
        DashboardMetric(
            "Candidates",
            system.candidatesFound?.toString() ?: "Unavailable",
        )
    }
}

@Composable
private fun AccountCard(account: AccountSummary) {
    SectionCard(
        title = "Account",
    ) {
        DashboardMetric(
            "Equity",
            formatCurrency(account.equity),
        )
        DashboardMetric(
            "Cash",
            formatCurrency(account.cash),
        )
        DashboardMetric(
            "Buying power",
            formatCurrency(account.buyingPower),
        )
        DashboardMetric(
            "Portfolio value",
            formatCurrency(account.portfolioValue),
        )
        DashboardMetric(
            "Long market value",
            formatCurrency(account.longMarketValue),
        )
        DashboardMetric(
            "Status",
            displayText(account.status),
        )
    }
}

@Composable
private fun PerformanceCard(
    performance: PerformanceSummary,
) {
    SectionCard(
        title = "Performance",
    ) {
        DashboardMetric(
            "Total realized P/L",
            formatCurrency(performance.totalRealizedPl),
        )
        DashboardMetric(
            "Win rate",
            formatPercent(performance.winRate),
        )
        DashboardMetric(
            "Average return",
            formatPercent(performance.averageReturn),
        )
        DashboardMetric(
            "Total trades",
            performance.totalTrades.toString(),
        )
        DashboardMetric(
            "Open trades",
            performance.openTrades.toString(),
        )
        DashboardMetric(
            "Closed trades",
            performance.closedTrades.toString(),
        )
        DashboardMetric(
            "Winners / Losers",
            "${performance.winners} / ${performance.losers}",
        )
    }
}

@Composable
private fun PositionsCard(
    positions: List<PositionSummary>,
    onPositionSelected: (PositionSummary) -> Unit,
) {
    SectionCard(
        title = "Open Positions (${positions.size})",
    ) {
        if (positions.isEmpty()) {
            Text("No open positions")
        } else {
            positions.forEachIndexed { index, position ->
                Text(
                    text = position.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                DashboardMetric(
                    "Side / Quantity",
                    "${displayText(position.side)} / ${formatQuantity(position.quantity)}",
                )
                DashboardMetric(
                    "Entry",
                    formatCurrency(position.entryPrice),
                )
                DashboardMetric(
                    "Current",
                    formatCurrency(position.currentPrice),
                )
                DashboardMetric(
                    "Market value",
                    formatCurrency(position.marketValue),
                )
                DashboardMetric(
                    "Unrealized P/L",
                    formatSignedCurrency(position.unrealizedPl),
                )
                DashboardMetric(
                    "Change today",
                    formatPercent(position.changeToday * 100),
                )

                position.score?.let { score ->
                    DashboardMetric(
                        "AI score",
                        score.format(1),
                    )
                }

                position.stopPrice?.let { stopPrice ->
                    DashboardMetric(
                        "Stop",
                        formatCurrency(stopPrice),
                    )
                }

                position.targetPrice?.let { targetPrice ->
                    DashboardMetric(
                        "Target",
                        formatCurrency(targetPrice),
                    )
                }

                position.riskAmount?.let { riskAmount ->
                    DashboardMetric(
                        "Planned risk",
                        formatCurrency(riskAmount),
                    )
                }

                position.riskPerShare?.let { riskPerShare ->
                    DashboardMetric(
                        "Risk per share",
                        formatCurrency(riskPerShare),
                    )
                }

                position.rewardToRisk?.let { rewardToRisk ->
                    DashboardMetric(
                        "Reward / Risk",
                        rewardToRisk.format(2),
                    )
                }

                position.plannedStopDistancePercent?.let {
                    stopDistance ->
                    DashboardMetric(
                        "Stop distance",
                        formatPercent(stopDistance),
                    )
                }

                position.stopMethod?.let { stopMethod ->
                    DashboardMetric(
                        "Stop method",
                        displayText(stopMethod),
                    )
                }

                position.targetMethod?.let { targetMethod ->
                    DashboardMetric(
                        "Target method",
                        displayText(targetMethod),
                    )
                }

                position.brokerStatus?.let { brokerStatus ->
                    DashboardMetric(
                        "Broker status",
                        displayText(brokerStatus),
                    )
                }

                position.tradePlanVersion?.let { version ->
                    DashboardMetric(
                        "Trade plan",
                        "Version $version",
                    )
                }

                Button(
                    onClick = {
                        onPositionSelected(position)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View ${position.symbol} Details")
                }

                if (index < positions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersCard(
    orders: List<OrderSummary>,
) {
    SectionCard(
        title = "Open Orders (${orders.size})",
    ) {
        if (orders.isEmpty()) {
            Text("No open orders")
        } else {
            orders.forEachIndexed { index, order ->
                Text(
                    text = order.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                DashboardMetric(
                    "Side / Type",
                    "${displayText(order.side)} / ${displayText(order.orderType)}",
                )
                DashboardMetric(
                    "Quantity",
                    formatQuantity(order.quantity),
                )
                DashboardMetric(
                    "Filled",
                    formatQuantity(order.filledQuantity),
                )
                DashboardMetric(
                    "Status",
                    displayText(order.status),
                )

                if (index < orders.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerResultsCard(
    results: List<ScannerResult>,
) {
    SectionCard(
        title = "Latest Scanner Results",
    ) {
        if (results.isEmpty()) {
            Text("No scanner results available")
        } else {
            results.forEachIndexed { index, result ->
                Text(
                    text = "${result.rank ?: index + 1}. ${result.ticker}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                DashboardMetric(
                    "Price",
                    formatCurrency(result.price),
                )
                DashboardMetric(
                    "AI score",
                    result.score.format(1),
                )
                DashboardMetric(
                    "Gain",
                    formatPercent(result.gainPercent),
                )
                DashboardMetric(
                    "RVOL",
                    result.rvol.format(2),
                )
                DashboardMetric(
                    "Alert qualified",
                    if (result.alertQualified) "YES" else "NO",
                )

                if (index < results.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTradesCard(
    trades: List<TradeSummary>,
) {
    SectionCard(
        title = "Recent Trades (${trades.size})",
    ) {
        if (trades.isEmpty()) {
            Text("No recent trades")
        } else {
            trades.forEachIndexed { index, trade ->
                Text(
                    text = trade.ticker,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                DashboardMetric(
                    "Entry",
                    formatCurrency(trade.entryPrice),
                )
                DashboardMetric(
                    "Exit",
                    trade.exitPrice?.let(::formatCurrency)
                        ?: "Unavailable",
                )
                DashboardMetric(
                    "Realized P/L",
                    trade.realizedPl?.let(::formatSignedCurrency)
                        ?: "Unavailable",
                )
                DashboardMetric(
                    "Return",
                    trade.realizedPlPercent?.let(::formatPercent)
                        ?: "Unavailable",
                )
                DashboardMetric(
                    "Exit reason",
                    trade.exitReason?.let(::displayText)
                        ?: "Unavailable",
                )
                DashboardMetric(
                    "Mode",
                    displayText(trade.executionMode),
                )

                if (index < trades.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider()

            content()
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale.US,
    ).format(value)
}

private fun formatSignedCurrency(value: Double): String {
    val formatted = formatCurrency(kotlin.math.abs(value))

    return when {
        value > 0 -> "+$formatted"
        value < 0 -> "-$formatted"
        else -> formatted
    }
}

private fun formatPercent(value: Double): String {
    return "${value.format(2)}%"
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.format(4)
    }
}

private fun displayText(value: String): String {
    return value
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { character ->
            character.titlecase(Locale.US)
        }
}

private fun Double.format(decimals: Int): String {
    return String.format(
        Locale.US,
        "%.${decimals}f",
        this,
    )
}


package com.example.markstradingscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MarksTradingScannerTheme {
                ScannerDashboardScreen()
            }
        }
    }
}

data class SystemStatus(
    val status: String,
    val executionMode: String,
    val marketSession: String,
    val scanningSession: Boolean,
    val scannerIntervalSeconds: Int,
    val latestScanId: Int?,
    val latestScanStatus: String?,
    val candidatesFound: Int?,
    val alertsSent: Int?,
    val brokerConnected: Boolean,
    val brokerHealthy: Boolean,
    val brokerMessage: String,
)

data class AccountSummary(
    val status: String,
    val currency: String,
    val cash: Double,
    val buyingPower: Double,
    val equity: Double,
    val portfolioValue: Double,
    val lastEquity: Double,
    val longMarketValue: Double,
    val patternDayTrader: Boolean,
    val tradingBlocked: Boolean,
)

data class DashboardData(
    val system: SystemStatus,
    val account: AccountSummary,
)

data class MetricRow(
    val label: String,
    val value: String,
)

@Composable
fun ScannerDashboardScreen() {
    var dashboardData by remember {
        mutableStateOf<DashboardData?>(null)
    }
    var loading by remember {
        mutableStateOf(true)
    }
    var errorText by remember {
        mutableStateOf<String?>(null)
    }
    var refreshRequest by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(refreshRequest) {
        loading = true
        errorText = null

        try {
            dashboardData = loadDashboardData()
        } catch (exception: Exception) {
            errorText = exception.message ?: "Unknown connection error"
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Mark's Trading Scanner",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "Mobile Dashboard",
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

                errorText != null -> {
                    item {
                        ErrorCard(
                            message = errorText ?: "Unknown error",
                            onRetry = {
                                refreshRequest += 1
                            },
                        )
                    }
                }

                dashboardData != null -> {
                    val data = dashboardData!!

                    item {
                        StatusCard(data.system)
                    }

                    item {
                        AccountCard(data.account)
                    }

                    item {
                        ScannerCard(data.system)
                    }

                    item {
                        BrokerCard(data.system)
                    }

                    item {
                        Button(
                            onClick = {
                                refreshRequest += 1
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Refresh Dashboard")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Connected securely through Tailscale",
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
                    text = "Connecting to scanner",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Loading system and account data...",
                    style = MaterialTheme.typography.bodyMedium,
                )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Connection failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun StatusCard(system: SystemStatus) {
    val scannerOnline = system.status.equals(
        other = "ok",
        ignoreCase = true,
    )

    val overallHealthy = (
        scannerOnline &&
            system.brokerConnected &&
            system.brokerHealthy
        )

    DashboardCard(
        title = "System Status",
        metrics = listOf(
            MetricRow(
                label = "Overall",
                value = if (overallHealthy) "ONLINE" else "ATTENTION",
            ),
            MetricRow(
                label = "Scanner API",
                value = if (scannerOnline) "ONLINE" else "OFFLINE",
            ),
            MetricRow(
                label = "Broker",
                value = if (system.brokerConnected) {
                    "CONNECTED"
                } else {
                    "DISCONNECTED"
                },
            ),
            MetricRow(
                label = "Broker health",
                value = if (system.brokerHealthy) {
                    "HEALTHY"
                } else {
                    "UNHEALTHY"
                },
            ),
        ),
    )
}

@Composable
private fun AccountCard(account: AccountSummary) {
    DashboardCard(
        title = "Account",
        metrics = listOf(
            MetricRow(
                label = "Equity",
                value = formatCurrency(account.equity),
            ),
            MetricRow(
                label = "Cash",
                value = formatCurrency(account.cash),
            ),
            MetricRow(
                label = "Buying power",
                value = formatCurrency(account.buyingPower),
            ),
            MetricRow(
                label = "Portfolio value",
                value = formatCurrency(account.portfolioValue),
            ),
            MetricRow(
                label = "Long market value",
                value = formatCurrency(account.longMarketValue),
            ),
            MetricRow(
                label = "Account status",
                value = account.status.uppercase(),
            ),
        ),
    )
}

@Composable
private fun ScannerCard(system: SystemStatus) {
    DashboardCard(
        title = "Scanner",
        metrics = listOf(
            MetricRow(
                label = "Market session",
                value = displayText(system.marketSession),
            ),
            MetricRow(
                label = "Execution mode",
                value = displayText(system.executionMode),
            ),
            MetricRow(
                label = "Scanning session",
                value = if (system.scanningSession) "ACTIVE" else "INACTIVE",
            ),
            MetricRow(
                label = "Scan interval",
                value = "${system.scannerIntervalSeconds} seconds",
            ),
            MetricRow(
                label = "Latest scan",
                value = system.latestScanId?.let { "#$it" } ?: "Unavailable",
            ),
            MetricRow(
                label = "Scan status",
                value = system.latestScanStatus?.let(::displayText)
                    ?: "Unavailable",
            ),
            MetricRow(
                label = "Candidates",
                value = system.candidatesFound?.toString() ?: "Unavailable",
            ),
            MetricRow(
                label = "Alerts sent",
                value = system.alertsSent?.toString() ?: "Unavailable",
            ),
        ),
    )
}

@Composable
private fun BrokerCard(system: SystemStatus) {
    DashboardCard(
        title = "Broker",
        metrics = listOf(
            MetricRow(
                label = "Connected",
                value = if (system.brokerConnected) "YES" else "NO",
            ),
            MetricRow(
                label = "Healthy",
                value = if (system.brokerHealthy) "YES" else "NO",
            ),
            MetricRow(
                label = "Message",
                value = system.brokerMessage.ifBlank {
                    "No broker message"
                },
            ),
        ),
    )
}

@Composable
private fun DashboardCard(
    title: String,
    metrics: List<MetricRow>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider()

            metrics.forEachIndexed { index, metric ->
                MetricLine(metric)

                if (index < metrics.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MetricLine(metric: MetricRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = metric.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

private suspend fun loadDashboardData(): DashboardData =
    withContext(Dispatchers.IO) {
        val systemJson = requestJson("/system/status")
        val accountJson = requestJson("/account")

        DashboardData(
            system = parseSystemStatus(systemJson),
            account = parseAccountSummary(accountJson),
        )
    }

private fun requestJson(path: String): JSONObject {
    val baseUrl = BuildConfig.MOBILE_API_BASE_URL.trimEnd('/')
    val apiKey = BuildConfig.MOBILE_API_KEY

    require(baseUrl.isNotBlank()) {
        "MOBILE_API_BASE_URL is not configured."
    }

    require(apiKey.isNotBlank()) {
        "MOBILE_API_KEY is not configured."
    }

    val connection = (
        URL("$baseUrl$path").openConnection()
            as HttpURLConnection
        )

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty(
            "X-API-Key",
            apiKey,
        )

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            error(
                "API returned HTTP $responseCode for $path. $errorBody"
            )
        }

        val responseBody = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        return JSONObject(responseBody)
    } finally {
        connection.disconnect()
    }
}

private fun parseSystemStatus(json: JSONObject): SystemStatus {
    val latestScan = json.optJSONObject("latest_scan")
    val broker = json.optJSONObject("broker")

    return SystemStatus(
        status = json.optString("status", "unknown"),
        executionMode = json.optString(
            "execution_mode",
            "unknown",
        ),
        marketSession = json.optString(
            "market_session",
            "unknown",
        ),
        scanningSession = json.optBoolean(
            "scanning_session",
            false,
        ),
        scannerIntervalSeconds = json.optInt(
            "scanner_interval_seconds",
            0,
        ),
        latestScanId = latestScan?.optInt("id"),
        latestScanStatus = latestScan?.optString(
            "status",
            "unknown",
        ),
        candidatesFound = latestScan?.optInt(
            "candidates_found",
        ),
        alertsSent = latestScan?.optInt(
            "alerts_sent",
        ),
        brokerConnected = broker?.optBoolean(
            "connected",
            false,
        ) ?: false,
        brokerHealthy = broker?.optBoolean(
            "healthy",
            false,
        ) ?: false,
        brokerMessage = broker?.optString(
            "message",
            "",
        ).orEmpty(),
    )
}

private fun parseAccountSummary(json: JSONObject): AccountSummary {
    return AccountSummary(
        status = json.optString("status", "unknown"),
        currency = json.optString("currency", "USD"),
        cash = json.optDouble("cash", 0.0),
        buyingPower = json.optDouble("buying_power", 0.0),
        equity = json.optDouble("equity", 0.0),
        portfolioValue = json.optDouble(
            "portfolio_value",
            0.0,
        ),
        lastEquity = json.optDouble("last_equity", 0.0),
        longMarketValue = json.optDouble(
            "long_market_value",
            0.0,
        ),
        patternDayTrader = json.optBoolean(
            "pattern_day_trader",
            false,
        ),
        tradingBlocked = json.optBoolean(
            "trading_blocked",
            false,
        ),
    )
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale.US,
    ).format(value)
}

private fun displayText(value: String): String {
    return value
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { character ->
            character.titlecase(Locale.US)
        }
}

package com.example.markstradingscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.markstradingscanner.ui.theme.MarksTradingScannerTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MarksTradingScannerTheme {
                ScannerStatusScreen()
            }
        }
    }
}

@Composable
fun ScannerStatusScreen() {
    var loading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            statusText = loadSystemStatus()
        } catch (exception: Exception) {
            errorText = exception.message ?: "Unknown connection error"
        } finally {
            loading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Mark's Trading Scanner",
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                text = "Mobile System Status",
                style = MaterialTheme.typography.titleMedium,
            )

            when {
                loading -> {
                    CircularProgressIndicator()
                    Text("Connecting to scanner...")
                }

                errorText != null -> {
                    Text(
                        text = "Connection failed",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(errorText ?: "")
                }

                else -> {
                    Text(statusText)
                }
            }
        }
    }
}

private suspend fun loadSystemStatus(): String = withContext(Dispatchers.IO) {
    val baseUrl = BuildConfig.MOBILE_API_BASE_URL.trimEnd('/')
    val apiKey = BuildConfig.MOBILE_API_KEY

    require(baseUrl.isNotBlank()) {
        "MOBILE_API_BASE_URL is not configured."
    }

    require(apiKey.isNotBlank()) {
        "MOBILE_API_KEY is not configured."
    }

    val connection = (
        URL("$baseUrl/system/status").openConnection()
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
                "API returned HTTP $responseCode. $errorBody"
            )
        }

        val responseBody = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        val json = JSONObject(responseBody)
        val latestScan = json.optJSONObject("latest_scan")
        val broker = json.optJSONObject("broker")

        buildString {
            appendLine(
                "System: ${json.optString("status", "unknown")}"
            )
            appendLine(
                "Execution mode: " +
                    json.optString("execution_mode", "unknown")
            )
            appendLine(
                "Market session: " +
                    json.optString("market_session", "unknown")
            )
            appendLine(
                "Scanning session: " +
                    json.optBoolean("scanning_session", false)
            )

            if (latestScan != null) {
                appendLine(
                    "Latest scan: #${latestScan.optInt("id")}"
                )
                appendLine(
                    "Scan status: " +
                        latestScan.optString("status", "unknown")
                )
                appendLine(
                    "Candidates: " +
                        latestScan.optInt("candidates_found")
                )
            }

            if (broker != null) {
                appendLine(
                    "Broker connected: " +
                        broker.optBoolean("connected", false)
                )
                appendLine(
                    "Broker healthy: " +
                        broker.optBoolean("healthy", false)
                )
            }
        }.trim()
    } finally {
        connection.disconnect()
    }
}

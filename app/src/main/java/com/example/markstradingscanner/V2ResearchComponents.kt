package com.example.markstradingscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun V2ResearchHost(onCockpit: () -> Unit, onEvidence: () -> Unit) {
    var result by remember { mutableStateOf(V2ResearchResult()) }
    var requestedDate by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(requestedDate, refresh) {
        loading = true
        val loaded = ScannerApiClient.loadV2Research(requestedDate)
        result = if (loaded.snapshot == null && result.snapshot != null) result.copy(unavailableReason = loaded.unavailableReason) else loaded
        loading = false
    }
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TopLevelSwitch("V2", onCockpit, onEvidence, {})
                Text("V2 Research")
                Text("Dry-run observations only. Separate from V1. No order controls.")
                OutlinedTextField(value = dateInput, onValueChange = { dateInput = it }, label = { Text("Eastern session YYYY-MM-DD; blank = latest") })
                Button(enabled = !loading, onClick = {
                    try { v2ResearchPath(dateInput); requestedDate = dateInput; refresh += 1 }
                    catch (_: Exception) { result = result.copy(unavailableReason = "Enter a valid YYYY-MM-DD date") }
                }) { Text(if (loading) "Loading…" else "Load / Refresh V2") }
                result.unavailableReason?.let { Text("Unavailable / stale: $it") }
            }
            val data = result.snapshot
            if (data != null) {
                item {
                    SectionCard("V2 status") {
                        DashboardMetric("Session", v2Value(data, "trading_date"))
                        DashboardMetric("Runner", v2Value(data, "runtime_status"))
                        DashboardMetric("Reported order authority", v2Value(data, "order_authority"))
                        DashboardMetric("Heartbeat", v2Time(v2Value(data, "last_heartbeat")))
                        DashboardMetric("Expected next window", v2Time(v2Value(data, "next_collection_window")))
                        Text("Waiting is not collecting. Missing or stale evidence is never a healthy status.")
                        val warnings = data.optJSONArray("warnings")
                        if (warnings != null) for (i in 0 until minOf(warnings.length(), 20)) Text(warnings.optString(i).take(160))
                    }
                }
                item {
                    SectionCard("Forward dry-run decisions") {
                        val paper = data.optJSONObject("paper") ?: JSONObject()
                        val counts = paper.optJSONObject("counts") ?: JSONObject()
                        for (key in listOf("WOULD_ENTER", "WAIT", "WOULD_REJECT", "BLOCKED", "UNKNOWN")) DashboardMetric(key, v2Value(counts, key))
                        Text("Would-enter is not a submitted order or fill. Latest 30 receipts.")
                        if (v2Rows(data,"paper").isEmpty()) Text(if (paper.optBoolean("available")) "No forward decisions for this session." else "Forward evidence unavailable.")
                    }
                }
                for (row in v2Rows(data,"paper")) item { V2ReceiptCard(row, true) }
                item { Text("Original shadow setup observations — distinct from forward decisions") }
                if (v2Rows(data,"shadow").isEmpty()) item { Text("No original shadow observations available for this session.") }
                for (row in v2Rows(data,"shadow")) item { V2ReceiptCard(row, false) }
                item {
                    SectionCard("Last V2 account snapshot") {
                        val account = data.optJSONObject("account_snapshot") ?: JSONObject()
                        Text(if (account.opt("fresh") == true) "Fresh snapshot" else "Historical / unavailable snapshot")
                        DashboardMetric("Observed", v2Time(v2Value(account,"observed_at")))
                        for (key in listOf("cash_usd","equity_usd","position_count","open_order_count")) DashboardMetric(key,v2Value(account,key))
                        Text("This page makes no broker request.")
                    }
                }
            } else if (!loading) item { Text("V2 evidence unavailable. No state is inferred.") }
        }
    }
}

@Composable
private fun V2ReceiptCard(row: JSONObject, forward: Boolean) {
    SectionCard(v2Value(row,"ticker") + " · " + v2Value(row,"setup_family")) {
        DashboardMetric("Verdict",v2Value(row,"verdict"))
        DashboardMetric("Setup state",v2Value(row,"setup_state"))
        DashboardMetric("Observed",v2Time(v2Value(row,"observed_at")))
        if (forward) {
            DashboardMetric("Evaluated",v2Time(v2Value(row,"evaluated_at")))
            DashboardMetric("Evaluation ask",v2Value(row,"evaluation_ask"))
            DashboardMetric("Spread / ask %",v2Value(row,"spread_percent_of_ask"))
            Text("No order submitted")
        }
        for (field in listOf("reason_codes","execution_reasons")) {
            val reasons = row.optJSONArray(field)
            if (reasons != null) for (i in 0 until minOf(reasons.length(),30)) Text(reasons.optString(i).take(160))
        }
        val gates = row.optJSONObject("gates") ?: JSONObject()
        for (key in listOf("evidence_is_fresh","spread_liquidity_acceptable","system_ready","risk_feasible","no_conflicting_position_or_order")) {
            val value = gates.optString(key)
            DashboardMetric(key, if (value in listOf("PASS","FAIL")) value else "UNKNOWN")
        }
    }
}

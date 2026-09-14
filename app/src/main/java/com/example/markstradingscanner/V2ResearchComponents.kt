package com.example.markstradingscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject

@Composable
fun V2ResearchHost(onCockpit: () -> Unit, onEvidence: () -> Unit) {
    var result by remember { mutableStateOf(V2ResearchResult()) }
    var requestedDate by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var loadedDate by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val owner = LocalContext.current as? LifecycleOwner
    var foreground by remember { mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> foreground = owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(foreground) {
        while (foreground && isActive) { nowMillis = System.currentTimeMillis(); delay(1000) }
    }
    LaunchedEffect(requestedDate, refresh, foreground) {
        if (!foreground) return@LaunchedEffect
        if (loadedDate != requestedDate) result = V2ResearchResult()
        while (isActive) {
            loading = true
            val loaded = ScannerApiClient.loadV2Research(requestedDate)
            result = if (loaded.snapshot == null && result.snapshot != null) result.copy(unavailableReason = loaded.unavailableReason) else loaded
            loadedDate = requestedDate
            loading = false
            delay(10000)
        }
    }
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TopLevelSwitch("V2", onCockpit, onEvidence, {})
                Text("V2 Paper Monitor", style = MaterialTheme.typography.headlineSmall)
                Text("Read-only · Separate from V1 · Refreshes every 10 seconds while open")
                result.unavailableReason?.let { Text("Connection unavailable — showing older evidence", color = MaterialTheme.colorScheme.error) }
            }
            val data = result.snapshot
            if (data != null) {
                val live = result.unavailableReason == null && v2SnapshotFresh(data, nowMillis)
                val account = data.optJSONObject("account_snapshot") ?: JSONObject()
                val health = data.optJSONObject("health") ?: JSONObject()
                val session = data.optJSONObject("session") ?: JSONObject()
                item {
                    SectionCard("Verified health") {
                        Text(if (live) "V2 is healthy" else "Live health is not verified", color = if (live) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        DashboardMetric("Runner", if (live) v2Label(v2Value(health,"runner")) else "Stale / unverified")
                        DashboardMetric("Protective exits", if (live) v2Label(v2Value(health,"protective_exits")) else "Stale / unverified")
                        DashboardMetric("Heartbeat", v2Age(data,"last_heartbeat",nowMillis))
                        DashboardMetric("Page evidence", v2Age(data,"generated_at",nowMillis))
                        DashboardMetric("Paper authority", if (live) v2Label(v2Value(data,"order_authority")) else "Not currently verified")
                        val warnings = data.optJSONArray("warnings")
                        if (warnings != null) for (i in 0 until minOf(warnings.length(),20)) Text(v2Label(warnings.optString(i)),color=MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    SectionCard("Today's session · Eastern time") {
                        DashboardMetric("Phase", if (live) v2Label(v2Value(session,"phase")) else "Schedule only — runtime unverified")
                        DashboardMetric("Entries", "5:00 AM–12:00 PM ET")
                        DashboardMetric("Flatten begins",v2Time(v2Value(session,"flatten_start")))
                        DashboardMetric("Flat target",v2Time(v2Value(session,"flat_target")))
                        DashboardMetric("Account flatness",if (live) v2Label(v2Value(session,"flatness")) else "Unverified")
                        DashboardMetric("Next scheduled start",v2Time(v2Value(data,"next_collection_window")))
                    }
                }
                item {
                    SectionCard("Paper account · " + v2Value(account,"trading_date")) {
                        Text(if (live && account.optBoolean("fresh")) "Current recorded account snapshot" else "Last recorded snapshot — not live",color=if(live) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        DashboardMetric("Equity USD",v2Value(account,"equity_usd"))
                        DashboardMetric("Cash USD",v2Value(account,"cash_usd"))
                        DashboardMetric("Today's P&L USD",v2Value(account,"daily_pnl_usd"))
                        DashboardMetric("Today's P&L %",v2Value(account,"daily_pnl_percent"))
                        Text("P&L is equity change from the recorded day-start balance; unavailable until that baseline exists.")
                        DashboardMetric("Broker position count",v2Value(account,"position_count"))
                        DashboardMetric("Broker open order count",v2Value(account,"open_order_count"))
                        DashboardMetric("Observed",v2Time(v2Value(account,"observed_at")))
                        Text("Details below are local ledger records, not new broker requests.")
                        val positions=account.optJSONArray("positions")
                        if (positions==null || positions.length()==0) Text(if(account.optBoolean("ledger_available")) "No filled positions recorded in ledger." else "Position details unavailable.")
                        if (positions!=null) for(i in 0 until minOf(positions.length(),30)) {
                            val row=positions.optJSONObject(i) ?: continue
                            DashboardMetric(v2Value(row,"ticker"),v2Value(row,"quantity")+" shares recorded")
                        }
                        val orders=account.optJSONArray("pending_orders")
                        if(orders!=null) for(i in 0 until minOf(orders.length(),30)) {
                            val row=orders.optJSONObject(i) ?: continue
                            Text(v2Value(row,"ticker")+" · "+v2Label(v2Value(row,"side"))+" · "+v2Label(v2Value(row,"state")))
                            Text("Filled "+v2Value(row,"filled")+" / "+v2Value(row,"requested"))
                        }
                    }
                }
                item {
                    SectionCard("Decision evidence") {
                        OutlinedTextField(value=dateInput,onValueChange={dateInput=it},label={Text("Session YYYY-MM-DD; blank = latest")})
                        Button(enabled=!loading,onClick={
                            try { v2ResearchPath(dateInput); requestedDate=dateInput; refresh+=1 }
                            catch(_:Exception){result=result.copy(unavailableReason="Enter a valid YYYY-MM-DD date")}
                        }) { Text(if(loading) "Refreshing…" else "Load / Refresh") }
                        DashboardMetric("Evidence date",v2Value(data,"trading_date"))
                        val counts=data.optJSONObject("paper")?.optJSONObject("counts") ?: JSONObject()
                        for(key in listOf("WOULD_ENTER","WAIT","WOULD_REJECT","BLOCKED")) DashboardMetric(v2Label(key),v2Value(counts,key))
                        Text("Would-enter is a signal, not a fill. Latest 30 forward receipts.")
                        if(v2Rows(data,"paper").isEmpty()) Text(if(data.optJSONObject("paper")?.optBoolean("available")==true) "No paper decisions recorded for this session." else "Paper decision evidence unavailable.")
                    }
                }
                for(row in v2Rows(data,"paper")) item { V2ReceiptCard(row,true) }
                item { Text("Detected setups · original shadow observations",style=MaterialTheme.typography.titleMedium) }
                if(v2Rows(data,"shadow").isEmpty()) item { Text("No setup observations available for this session.") }
                for(row in v2Rows(data,"shadow")) item { V2ReceiptCard(row,false) }
            } else item { Text(if(loading) "Loading V2…" else "V2 evidence unavailable. No health or account state is inferred.") }
        }
    }
}

@Composable
private fun V2ReceiptCard(row: JSONObject, forward: Boolean) {
    SectionCard(v2Value(row,"ticker")+" · "+v2Label(v2Value(row,"setup_family"))) {
        DashboardMetric("Decision",v2Label(v2Value(row,"verdict")))
        DashboardMetric("Setup",v2Label(v2Value(row,"setup_state")))
        DashboardMetric("Observed",v2Time(v2Value(row,"observed_at")))
        if(forward) {
            DashboardMetric("Entry evidence",v2Label(v2Value(row,"entry_status")))
            DashboardMetric("Main reason",v2Label(v2Value(row,"primary_reason")))
            row.optJSONObject("recorded_order")?.let { order ->
                DashboardMetric("Recorded filled shares",v2Value(order,"filled_quantity"))
                DashboardMetric("Average fill",v2Value(order,"average_fill"))
            }
            DashboardMetric("Evaluated",v2Time(v2Value(row,"evaluated_at")))
            DashboardMetric("Evaluation ask",v2Value(row,"evaluation_ask"))
            DashboardMetric("Spread / ask %",v2Value(row,"spread_percent_of_ask"))
        }
        for(field in listOf("reason_codes","execution_reasons")) {
            val reasons=row.optJSONArray(field)
            if(reasons!=null) for(i in 0 until minOf(reasons.length(),30)) Text(v2Label(reasons.optString(i).take(160)))
        }
    }
}

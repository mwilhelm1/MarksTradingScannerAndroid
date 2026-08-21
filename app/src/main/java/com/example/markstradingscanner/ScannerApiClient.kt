package com.example.markstradingscanner

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

object ScannerApiClient {
    suspend fun loadDashboard(): DashboardData =
        withContext(Dispatchers.IO) {
            DashboardData(
                system = parseSystemStatus(
                    requestJson("/system/status")
                ),
                account = parseAccount(
                    requestJson("/account")
                ),
                positions = parsePositions(
                    requestJson("/positions")
                ),
                orders = parseOrders(
                    requestJson("/orders")
                ),
                scannerResults = parseScannerResults(
                    requestJson("/scanner/results")
                ),
                trades = parseTrades(
                    requestJson("/trades")
                ),
                performance = parsePerformance(
                    requestJson("/performance")
                ),
                operations = loadOperations(),
            )
        }

    private fun loadOperations(): OperationsResult = try {
        OperationsResult(status = parseOperations(requestJson("/operations")))
    } catch (exception: Exception) {
        OperationsResult(
            unavailableReason = exception.message ?: "Operations request failed",
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
            connection.readTimeout = 15_000
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
                    "HTTP $responseCode from $path. $errorBody"
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
            latestScanId = latestScan?.nullableInt("id"),
            latestScanStatus = latestScan?.nullableString(
                "status"
            ),
            candidatesFound = latestScan?.nullableInt(
                "candidates_found"
            ),
            alertsSent = latestScan?.nullableInt(
                "alerts_sent"
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

    private fun parseAccount(json: JSONObject): AccountSummary {
        return AccountSummary(
            status = json.optString("status", "unknown"),
            cash = json.optDouble("cash", 0.0),
            buyingPower = json.optDouble(
                "buying_power",
                0.0,
            ),
            equity = json.optDouble("equity", 0.0),
            portfolioValue = json.optDouble(
                "portfolio_value",
                0.0,
            ),
            longMarketValue = json.optDouble(
                "long_market_value",
                0.0,
            ),
        )
    }

    private fun parsePositions(
        json: JSONObject,
    ): List<PositionSummary> {
        val array = json.optJSONArray("positions")
            ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                add(
                    PositionSummary(
                        symbol = item.optString(
                            "symbol",
                            "UNKNOWN",
                        ),
                        side = item.optString(
                            "side",
                            "unknown",
                        ),
                        quantity = item.optDouble(
                            "quantity",
                            0.0,
                        ),
                        entryPrice = item.optDouble(
                            "entry_price",
                            0.0,
                        ),
                        currentPrice = item.optDouble(
                            "current_price",
                            0.0,
                        ),
                        marketValue = item.optDouble(
                            "market_value",
                            0.0,
                        ),
                        unrealizedPl = item.optDouble(
                            "unrealized_pl",
                            0.0,
                        ),
                        changeToday = item.optDouble(
                            "change_today",
                            0.0,
                        ),

                        localTradeId = (
                            if (item.isNull("local_trade_id")) {
                                null
                            } else {
                                item.optInt("local_trade_id")
                            }
                        ),
                        stopPrice = (
                            if (item.isNull("stop_price")) {
                                null
                            } else {
                                item.optDouble("stop_price")
                            }
                        ),
                        targetPrice = (
                            if (item.isNull("target_price")) {
                                null
                            } else {
                                item.optDouble("target_price")
                            }
                        ),
                        riskAmount = (
                            if (item.isNull("risk_amount")) {
                                null
                            } else {
                                item.optDouble("risk_amount")
                            }
                        ),
                        riskPerShare = (
                            if (item.isNull("risk_per_share")) {
                                null
                            } else {
                                item.optDouble("risk_per_share")
                            }
                        ),
                        score = (
                            if (item.isNull("score")) {
                                null
                            } else {
                                item.optDouble("score")
                            }
                        ),
                        stopMethod = (
                            if (item.isNull("stop_method")) {
                                null
                            } else {
                                item.optString("stop_method")
                                    .takeIf { it.isNotBlank() }
                            }
                        ),
                        targetMethod = (
                            if (item.isNull("target_method")) {
                                null
                            } else {
                                item.optString("target_method")
                                    .takeIf { it.isNotBlank() }
                            }
                        ),
                        rewardToRisk = (
                            if (item.isNull("reward_to_risk")) {
                                null
                            } else {
                                item.optDouble("reward_to_risk")
                            }
                        ),
                        plannedStopDistancePercent = (
                            if (
                                item.isNull(
                                    "planned_stop_distance_percent"
                                )
                            ) {
                                null
                            } else {
                                item.optDouble(
                                    "planned_stop_distance_percent"
                                )
                            }
                        ),
                        tradePlanVersion = (
                            if (item.isNull("trade_plan_version")) {
                                null
                            } else {
                                item.optString("trade_plan_version")
                                    .takeIf { it.isNotBlank() }
                            }
                        ),
                        brokerStatus = (
                            if (item.isNull("broker_status")) {
                                null
                            } else {
                                item.optString("broker_status")
                                    .takeIf { it.isNotBlank() }
                            }
                        ),
                    )
                )
            }
        }
    }

    private fun parseOrders(
        json: JSONObject,
    ): List<OrderSummary> {
        val array = json.optJSONArray("orders")
            ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                add(
                    OrderSummary(
                        symbol = item.optString(
                            "symbol",
                            "UNKNOWN",
                        ),
                        side = item.optString(
                            "side",
                            "unknown",
                        ),
                        orderType = item.optString(
                            "order_type",
                            "unknown",
                        ),
                        quantity = item.optDouble(
                            "quantity",
                            0.0,
                        ),
                        filledQuantity = item.optDouble(
                            "filled_quantity",
                            0.0,
                        ),
                        status = item.optString(
                            "status",
                            "unknown",
                        ),
                        submittedAt = item.nullableString(
                            "submitted_at"
                        ),
                    )
                )
            }
        }
    }

    private fun parseScannerResults(
        json: JSONObject,
    ): List<ScannerResult> {
        val array = json.optJSONArray("results")
            ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                add(
                    ScannerResult(
                        rank = item.nullableInt(
                            "rank_position"
                        ),
                        ticker = item.optString(
                            "ticker",
                            "UNKNOWN",
                        ),
                        price = item.optDouble(
                            "price",
                            0.0,
                        ),
                        score = item.optDouble(
                            "score",
                            0.0,
                        ),
                        gainPercent = item.optDouble(
                            "gain_percent",
                            0.0,
                        ),
                        rvol = item.optDouble(
                            "rvol",
                            0.0,
                        ),
                        alertQualified = item.optInt(
                            "alert_qualified",
                            0,
                        ) == 1,
                    )
                )
            }
        }
    }

    private fun parseTrades(
        json: JSONObject,
    ): List<TradeSummary> {
        val array = json.optJSONArray("trades")
            ?: return emptyList()

        return buildList {
            val displayLimit = minOf(array.length(), 20)

            for (index in 0 until displayLimit) {
                val item = array.optJSONObject(index) ?: continue

                add(
                    TradeSummary(
                        id = item.optInt("id", 0),
                        ticker = item.optString(
                            "ticker",
                            "UNKNOWN",
                        ),
                        entryPrice = item.optDouble(
                            "entry_price",
                            0.0,
                        ),
                        exitPrice = item.nullableDouble(
                            "exit_price"
                        ),
                        realizedPl = item.nullableDouble(
                            "realized_pl"
                        ),
                        realizedPlPercent = item.nullableDouble(
                            "realized_pl_percent"
                        ),
                        exitReason = item.nullableString(
                            "exit_reason"
                        ),
                        exitTime = item.nullableString(
                            "exit_time"
                        ),
                        executionMode = item.optString(
                            "execution_mode",
                            "unknown",
                        ),
                    )
                )
            }
        }
    }

    private fun parsePerformance(
        json: JSONObject,
    ): PerformanceSummary {
        return PerformanceSummary(
            totalTrades = json.optInt("total_trades", 0),
            openTrades = json.optInt("open_trades", 0),
            closedTrades = json.optInt("closed_trades", 0),
            winners = json.optInt("winners", 0),
            losers = json.optInt("losers", 0),
            winRate = json.optDouble("win_rate", 0.0),
            totalRealizedPl = json.optDouble(
                "total_realized_pl",
                0.0,
            ),
            averageReturn = json.optDouble(
                "average_return",
                0.0,
            ),
        )
    }

    internal fun parseOperations(json: JSONObject): OperationsStatus {
        val reliability = json.optJSONObject("daily_reliability")
            ?: error("Operations response omitted daily_reliability")
        val readiness = json.optJSONObject("trading_readiness")
            ?: error("Operations response omitted trading_readiness")
        val trading = json.optJSONObject("trading")
            ?: error("Operations response omitted trading")
        val broker = json.optJSONObject("broker")
            ?: error("Operations response omitted broker")
        val synchronization = broker.optJSONObject("synchronization_status")
            ?: error("Operations response omitted reconciliation status")

        val holds = json.optJSONArray("recovery_hold_surveillance")
            .objects { item ->
                RecoveryHoldSummary(
                    tradeId = item.optInt("trade_id"),
                    ticker = item.optString("ticker", "UNKNOWN"),
                    holdReason = item.nullableString("hold_reason"),
                    responsibilityState = item.optString("responsibility_state", "UNKNOWN"),
                    protectionOwner = item.optString("protection_owner", "UNKNOWN"),
                    brokerLocalQuantityMatch = item.optBoolean("broker_local_quantity_match"),
                    hardStopProtectionActive = item.optBoolean("hard_stop_protection_active"),
                    newEntriesBlocked = item.optBoolean("new_entries_blocked"),
                    requiresOperatorAction = item.optBoolean("requires_operator_action"),
                )
            }
        val responsibilities = json.optJSONArray("position_responsibilities")
            .objects { item ->
                PositionResponsibilitySummary(
                    tradeId = item.nullableInt("local_trade_id"),
                    ticker = item.optString("ticker", "UNKNOWN"),
                    state = item.optString("state", "UNKNOWN"),
                    protectionOwner = item.optString("protection_owner", "UNKNOWN"),
                    requiresOperatorAction = item.optBoolean("requires_operator_action"),
                    blocksNewEntries = item.optBoolean("blocks_new_entries"),
                )
            }
        val globalAction = json.optJSONObject("global_status")
            ?.optString("status")
            ?.equals("Operator Action Required", ignoreCase = true) == true

        return OperationsStatus(
            generatedAt = json.nullableString("generated_at"),
            dailyReliability = DailyReliabilitySummary(
                status = reliability.optString("daily_reliability_status", "UNKNOWN"),
                atRiskReasons = reliability.optJSONArray("at_risk_reasons").strings(),
                failureReasons = reliability.optJSONArray("failure_reasons").strings(),
            ),
            tradingReady = readiness.optBoolean("ready_for_new_entries"),
            operatorActionRequired = globalAction
                || holds.any { it.requiresOperatorAction }
                || responsibilities.any { it.requiresOperatorAction },
            brokerPositions = trading.optInt("open_positions"),
            brokerOpenOrders = broker.optInt("open_orders"),
            reconciliationClean = synchronization.optBoolean("synchronized"),
            recoveryHolds = holds,
            responsibilities = responsibilities,
            postFillRisks = json.optJSONArray("post_fill_excess_risk")
                .objects { item ->
                    PostFillRiskSummary(
                        tradeId = item.nullableInt("trade_id"),
                        ticker = item.optString("ticker", "UNKNOWN"),
                        riskAmount = item.optDouble("risk_amount"),
                        configuredLimit = item.optDouble("configured_limit"),
                        managementState = item.optString("management_state", "UNKNOWN"),
                    )
                },
        )
    }
}

private fun <T> JSONArray?.objects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(transform(it)) }
        }
    }
}

private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun JSONObject.nullableString(
    name: String,
): String? {
    if (!has(name) || isNull(name)) {
        return null
    }

    return optString(name).takeIf {
        it.isNotBlank() && it != "null"
    }
}

private fun JSONObject.nullableInt(
    name: String,
): Int? {
    if (!has(name) || isNull(name)) {
        return null
    }

    return optInt(name)
}

private fun JSONObject.nullableDouble(
    name: String,
): Double? {
    if (!has(name) || isNull(name)) {
        return null
    }

    return optDouble(name)
}


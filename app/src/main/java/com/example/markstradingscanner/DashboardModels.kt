package com.example.markstradingscanner

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
    val cash: Double,
    val buyingPower: Double,
    val equity: Double,
    val portfolioValue: Double,
    val longMarketValue: Double,
)

data class PositionSummary(
    val symbol: String,
    val side: String,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val marketValue: Double,
    val unrealizedPl: Double,
    val changeToday: Double,

    val localTradeId: Int?,
    val stopPrice: Double?,
    val targetPrice: Double?,
    val riskAmount: Double?,
    val riskPerShare: Double?,
    val score: Double?,
    val stopMethod: String?,
    val targetMethod: String?,
    val rewardToRisk: Double?,
    val plannedStopDistancePercent: Double?,
    val tradePlanVersion: String?,
    val brokerStatus: String?,
)

data class OrderSummary(
    val symbol: String,
    val side: String,
    val orderType: String,
    val quantity: Double,
    val filledQuantity: Double,
    val status: String,
    val submittedAt: String?,
)

data class ScannerResult(
    val rank: Int?,
    val ticker: String,
    val price: Double,
    val score: Double,
    val gainPercent: Double,
    val rvol: Double,
    val alertQualified: Boolean,
)

data class TradeSummary(
    val id: Int,
    val ticker: String,
    val entryPrice: Double,
    val exitPrice: Double?,
    val realizedPl: Double?,
    val realizedPlPercent: Double?,
    val exitReason: String?,
    val exitTime: String?,
    val executionMode: String,
)

data class PerformanceSummary(
    val totalTrades: Int,
    val openTrades: Int,
    val closedTrades: Int,
    val winners: Int,
    val losers: Int,
    val winRate: Double,
    val totalRealizedPl: Double,
    val averageReturn: Double,
)

data class DailyReliabilitySummary(
    val status: String?,
    val atRiskReasons: List<String>,
    val failureReasons: List<String>,
)

data class CleanTradingDaySummary(
    val status: String?,
    val completed: Boolean?,
    val reasons: List<String>,
    val tradingImpact: String?,
    val manualInterventionRequired: Boolean?,
    val historicalStreakAvailable: Boolean?,
)

data class TodaySummary(
    val tradeAttempts: Int?,
    val filledTrades: Int?,
    val enteredTrades: Int?,
    val completedTrades: Int?,
    val winners: Int?,
    val losers: Int?,
    val realizedPl: Double?,
    val unrealizedPl: Double?,
    val openPositions: Int?,
    val brokerOpenOrders: Int?,
)

data class LatestTradeSummary(
    val tradeId: Int?,
    val ticker: String?,
    val entryTime: String?,
    val exitTime: String?,
    val entryPrice: Double?,
    val exitPrice: Double?,
    val realizedPl: Double?,
    val exitReason: String?,
    val status: String?,
)

data class ActionableIncidentSummary(
    val code: String?,
    val severity: String?,
    val summary: String?,
    val operatorActionRequired: Boolean?,
    val tradingImpact: String?,
)

data class ComponentHealthSummary(
    val component: String,
    val status: String?,
)

data class EodStatusSummary(
    val phase: String?,
    val flatness: String?,
    val verification: String?,
)

data class RecoveryHoldSummary(
    val tradeId: Int,
    val ticker: String,
    val holdReason: String?,
    val responsibilityState: String,
    val protectionOwner: String,
    val brokerLocalQuantityMatch: Boolean,
    val hardStopProtectionActive: Boolean,
    val newEntriesBlocked: Boolean,
    val requiresOperatorAction: Boolean,
)

data class PositionResponsibilitySummary(
    val tradeId: Int?,
    val ticker: String,
    val state: String,
    val protectionOwner: String,
    val requiresOperatorAction: Boolean,
    val blocksNewEntries: Boolean,
)

data class PostFillRiskSummary(
    val tradeId: Int?,
    val ticker: String,
    val riskAmount: Double,
    val configuredLimit: Double,
    val managementState: String,
)

data class OperationsStatus(
    val generatedAt: String?,
    val overallStatus: String?,
    val dailyReliability: DailyReliabilitySummary,
    val cleanTradingDay: CleanTradingDaySummary?,
    val todaySummary: TodaySummary?,
    val latestTrade: LatestTradeSummary?,
    val actionableIncidents: List<ActionableIncidentSummary>,
    val componentHealth: List<ComponentHealthSummary>,
    val tradingReady: Boolean?,
    val operatorActionRequired: Boolean?,
    val brokerPositions: Int?,
    val brokerOpenOrders: Int?,
    val reconciliationClean: Boolean?,
    val unresolvedUnknownIntents: Int?,
    val unresolvedAmbiguousIntents: Int?,
    val eodStatus: EodStatusSummary?,
    val recoveryHolds: List<RecoveryHoldSummary>,
    val responsibilities: List<PositionResponsibilitySummary>,
    val postFillRisks: List<PostFillRiskSummary>,
)

data class OperationsResult(
    val status: OperationsStatus? = null,
    val unavailableReason: String? = null,
)

data class DashboardData(
    val system: SystemStatus,
    val account: AccountSummary,
    val positions: List<PositionSummary>,
    val orders: List<OrderSummary>,
    val scannerResults: List<ScannerResult>,
    val trades: List<TradeSummary>,
    val performance: PerformanceSummary,
    val operations: OperationsResult,
)



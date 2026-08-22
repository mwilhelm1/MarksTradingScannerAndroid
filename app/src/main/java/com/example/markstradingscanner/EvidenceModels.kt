package com.example.markstradingscanner

data class EvidenceResult(
    val snapshot: EvidenceSnapshot? = null,
    val unavailableReason: String? = null,
)

data class EvidenceSnapshot(
    val generatedAt: String?,
    val status: EvidenceStatus,
    val overview: EvidencePerformance,
    val sessions: List<EvidencePerformance>,
    val tradeBehavior: EvidenceTradeBehavior,
    val exits: List<EvidencePerformance>,
    val reEntries: EvidenceReEntries,
    val strategyIntelligence: StrategyIntelligenceEvidence,
    val currentStrategy: CurrentStrategyEvidence,
    val cleanTradingDays: CleanDayHistoryEvidence,
    val dataQuality: EvidenceDataQuality,
)

data class EvidenceStatus(
    val state: String?,
    val authoritativeVerdict: String?,
    val independentEvents: Int?,
    val tradingDays: Int?,
    val failedGates: List<String>,
    val warnings: List<String>,
    val semantics: String?,
)

data class EvidencePerformance(
    val group: String? = null,
    val tradeCount: Int? = null,
    val totalClosedTrades: Int? = null,
    val eligibleTrades: Int? = null,
    val excludedTrades: Int? = null,
    val realizedPl: Double? = null,
    val winRatePercent: Double? = null,
    val averageWinner: Double? = null,
    val averageLoser: Double? = null,
    val profitFactor: String? = null,
    val averageReturnPercent: Double? = null,
    val averageMfePercent: Double? = null,
    val averageMaePercent: Double? = null,
    val averageHoldMinutes: Double? = null,
)

data class EvidenceTradeBehavior(
    val averageHoldMinutes: Double?,
    val averageMfePercent: Double?,
    val averageMaePercent: Double?,
    val captureEfficiency: CaptureEfficiencyEvidence?,
)

data class CaptureEfficiencyEvidence(
    val label: String?,
    val percent: Double?,
    val sampleSize: Int?,
    val semantics: String?,
)

data class EvidenceReEntries(
    val scope: String?,
    val buckets: List<EvidencePerformance>,
    val sameSessionAvailable: Boolean?,
    val sameSessionReason: String?,
)

data class StrategyIntelligenceEvidence(
    val cohortName: String?,
    val legacyObservations: Int?,
    val correctedObservations: Int?,
    val correctedOutcomes: Int?,
    val legacyIncludedInPerformance: Boolean?,
    val setups: List<SetupEvidence>,
    val warnings: List<String>,
)

data class SetupEvidence(
    val setupType: String,
    val independentEvents: Int?,
    val controls: Int?,
    val outcomeCount: Int?,
    val maturityStatus: String?,
    val failedMaturityGates: List<String>,
)

data class CurrentStrategyEvidence(
    val tradePlanVersions: List<String>,
    val detectorVersion: String?,
    val outcomeCalculationVersion: String?,
)

data class CleanDayHistoryEvidence(
    val available: Boolean?,
    val count: Int?,
    val reason: String?,
)

data class EvidenceDataQuality(
    val includedTrades: Int?,
    val excludedTrades: Int?,
    val exclusionReasons: Map<String, Int>,
    val metricSampleSizes: Map<String, Int>,
    val unclassifiedSessionCount: Int?,
    val cohortLabels: Map<String, String>,
    val warnings: List<String>,
)

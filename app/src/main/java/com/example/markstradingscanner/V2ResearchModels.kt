package com.example.markstradingscanner

import org.json.JSONObject
import java.time.LocalDate

data class V2ResearchResult(val snapshot: JSONObject? = null, val unavailableReason: String? = null)

internal fun v2ResearchPath(date: String): String {
    if (date.isBlank()) return "/v2/research"
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) { "Use YYYY-MM-DD" }
    LocalDate.parse(date)
    return "/v2/research?trading_date=$date"
}

internal fun v2Value(json: JSONObject, key: String): String =
    if (json.isNull(key)) "Unavailable" else json.opt(key)?.toString()?.take(160) ?: "Unavailable"

internal fun v2Rows(json: JSONObject, section: String): List<JSONObject> {
    val rows = json.optJSONObject(section)?.optJSONArray("rows") ?: return emptyList()
    return (0 until minOf(rows.length(), 30)).mapNotNull { rows.optJSONObject(it) }
}

internal fun v2Time(value: String): String = try {
    java.time.OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.of("America/New_York"))
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")) + " ET"
} catch (_: Exception) { "Unavailable" }

internal fun v2SnapshotFresh(data: JSONObject, nowMillis: Long = System.currentTimeMillis()): Boolean {
    fun recent(key: String): Boolean = try {
        val age = nowMillis - java.time.OffsetDateTime.parse(data.getString(key)).toInstant().toEpochMilli()
        age in 0..15000
    } catch (_: Exception) { false }
    return recent("generated_at") && recent("last_heartbeat") && data.optJSONObject("health")?.optBoolean("healthy") == true
}

internal fun v2Label(code: String): String = code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

internal fun v2Age(data: JSONObject, key: String, nowMillis: Long): String = try {
    val seconds = (nowMillis - java.time.OffsetDateTime.parse(data.getString(key)).toInstant().toEpochMilli()) / 1000
    if (seconds < 0) "Unverified timestamp" else "$seconds seconds ago"
} catch (_: Exception) { "Unavailable" }

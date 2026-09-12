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

package com.example.markstradingscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class AvailableUpdate(
    val versionName: String,
    val downloadUrl: String,
)

object AndroidUpdateManager {
    suspend fun check(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val repository = BuildConfig.ANDROID_UPDATE_REPOSITORY.trim()
        if (repository.isBlank()) return@withContext null

        val releases = requestArray(
            "https://api.github.com/repos/$repository/releases?per_page=20"
        )
        for (index in 0 until releases.length()) {
            val release = releases.optJSONObject(index) ?: continue
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
            val tag = release.optString("tag_name")
            if (!tag.startsWith("android-v")) continue
            val version = tag.removePrefix("android-v")
            if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) {
                return@withContext null
            }
            val assets = release.optJSONArray("assets") ?: continue
            for (assetIndex in 0 until assets.length()) {
                val asset = assets.optJSONObject(assetIndex) ?: continue
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    return@withContext AvailableUpdate(
                        versionName = version,
                        downloadUrl = asset.optString("browser_download_url"),
                    )
                }
            }
        }
        null
    }

    suspend fun download(context: Context, update: AvailableUpdate): File =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "updates").apply { mkdirs() }
            val target = File(directory, "marks-trading-scanner-${update.versionName}.apk")
            val connection = open(update.downloadUrl)
            try {
                if (connection.responseCode !in 200..299) {
                    error("Update download failed with HTTP ${connection.responseCode}")
                }
                connection.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                connection.disconnect()
            }
            target
        }

    fun launchInstaller(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            apk,
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    internal fun compareVersions(left: String, right: String): Int {
        val a = left.split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrNull(index) ?: 0)
                .compareTo(b.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun requestArray(url: String): JSONArray {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) {
                error("GitHub release check failed with HTTP ${connection.responseCode}")
            }
            return JSONArray(
                connection.inputStream.bufferedReader().use { it.readText() }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MarksTradingScanner-Android")
        }
}

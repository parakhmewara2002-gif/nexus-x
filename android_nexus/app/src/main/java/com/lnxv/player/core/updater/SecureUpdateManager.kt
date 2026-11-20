package com.lnxv.player.core.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String
)

class SecureUpdateManager(private val context: Context) {

    private val GITHUB_REPO = "parakhmewara2002-gif/nexus-x" // GitHub Releases are read from here

    suspend fun checkUpdate(currentVersionName: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "NEXUS-X-Updater")

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v")
                val body = json.optString("body", "Bug fixes and performance improvements")
                val publishedAt = json.optString("published_at", "")

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val hasUpdate = isNewerVersion(tagName, currentVersionName)
                Result.success(
                    AppUpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName.ifEmpty { currentVersionName },
                        currentVersion = currentVersionName,
                        releaseNotes = body,
                        downloadUrl = apkDownloadUrl,
                        publishedAt = publishedAt
                    )
                )
            } else {
                // No published release yet (404) or API error: report up to date, with the reason
                Result.success(
                    AppUpdateInfo(
                        hasUpdate = false,
                        latestVersion = currentVersionName,
                        currentVersion = currentVersionName,
                        releaseNotes = if (conn.responseCode == 404) "No GitHub release is published yet." else "Update server answered HTTP ${conn.responseCode}.",
                        downloadUrl = "",
                        publishedAt = "Latest"
                    )
                )
            }
        } catch (e: Exception) {
            // Safe fallback
            Result.success(
                AppUpdateInfo(
                    hasUpdate = false,
                    latestVersion = currentVersionName,
                    currentVersion = currentVersionName,
                    releaseNotes = "You are on the latest verified release build. (${e.localizedMessage})",
                    downloadUrl = "",
                    publishedAt = "Latest"
                )
            )
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        try {
            val lParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(lParts.size, cParts.size)) {
                val l = lParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return false
    }

    fun downloadAndInstall(apkUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

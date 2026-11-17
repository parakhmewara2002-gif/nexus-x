package com.lnxv.player.core.device

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppInfoItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val sourceDir: String,
    val apkSize: Long
)

class AppManager(private val context: Context) {

    suspend fun getInstalledApps(includeSystem: Boolean = false): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val list = mutableListOf<AppInfoItem>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) continue

            val appName = pm.getApplicationLabel(appInfo).toString()
            val apkFile = File(appInfo.sourceDir)
            val size = if (apkFile.exists()) apkFile.length() else 0L
            val verCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            list.add(
                AppInfoItem(
                    appName = appName,
                    packageName = pkg.packageName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = verCode,
                    isSystemApp = isSystem,
                    sourceDir = appInfo.sourceDir,
                    apkSize = size
                )
            )
        }
        list.sortedBy { it.appName.lowercase() }
    }

    suspend fun backupApk(app: AppInfoItem, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val src = File(app.sourceDir)
            if (!src.exists()) return@withContext Result.failure(Exception("Source APK not found"))
            if (!destDir.exists()) destDir.mkdirs()
            val target = File(destDir, "${app.packageName}_v${app.versionName}.apk")
            src.copyTo(target, overwrite = true)
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

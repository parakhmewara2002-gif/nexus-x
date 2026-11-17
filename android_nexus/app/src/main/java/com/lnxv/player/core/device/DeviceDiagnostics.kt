package com.lnxv.player.core.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.RandomAccessFile

data class DeviceSpec(
    val manufacturer: String = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
    val model: String = Build.MODEL,
    val deviceName: String = Build.DEVICE,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val buildId: String = Build.DISPLAY,
    val cpuAbi: String = Build.SUPPORTED_ABIS.joinToString(", "),
    val totalRamBytes: Long,
    val availRamBytes: Long,
    val totalStorageBytes: Long,
    val availStorageBytes: Long,
    val batteryPct: Int,
    val isCharging: Boolean,
    val networkType: String,
    val isRooted: Boolean,
    val sensorCount: Int
)

class DeviceDiagnostics(private val context: Context) {

    fun checkRoot(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val exit = process.waitFor()
            exit == 0
        } catch (_: Exception) {
            false
        }
    }

    fun checkShizukuAvailable(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getDeviceSpec(): DeviceSpec {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorage = statFs.blockCountLong * statFs.blockSizeLong
        val availStorage = statFs.availableBlocksLong * statFs.blockSizeLong

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = connManager.activeNetwork
        val caps = connManager.getNetworkCapabilities(activeNet)
        val netType = when {
            caps == null -> "Disconnected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorCount = sensorManager.getSensorList(Sensor.TYPE_ALL).size

        return DeviceSpec(
            totalRamBytes = memInfo.totalMem,
            availRamBytes = memInfo.availMem,
            totalStorageBytes = totalStorage,
            availStorageBytes = availStorage,
            batteryPct = batteryPct,
            isCharging = isCharging,
            networkType = netType,
            isRooted = checkRoot(),
            sensorCount = sensorCount
        )
    }
}

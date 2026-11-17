package com.lnxv.player.core.adb

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress

data class CommandResult(
    val command: String,
    val output: String,
    val exitCode: Int,
    val executionTimeMs: Long
)

data class WirelessDebugInfo(
    val ipAddress: String,
    val port: Int = 5555,
    val isEnabled: Boolean,
    val adbTcpPortProp: String
)

data class ProcessItem(
    val pid: String,
    val user: String,
    val name: String,
    val cpuPercent: String = "0%",
    val memRss: String = "0K"
)

data class GuiAdbCommand(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rawCommand: String,
    val isDestructive: Boolean = false,
    val compatibilityStatus: String = "VERIFIED",
    val iconType: String = "terminal"
)

data class AdbDeviceState(
    val isUsbDebuggingEnabled: Boolean,
    val wirelessDebugInfo: WirelessDebugInfo,
    val selinuxMode: String,
    val activeProperties: Map<String, String>
)

class AdbTerminalEngine(private val context: Context? = null) {

    val builtInGuiCommands = listOf(
        GuiAdbCommand(
            id = "wlan_ip",
            title = "Network Interface Details",
            description = "Inspect local IPv4/IPv6 addresses and link routing",
            category = "Network",
            rawCommand = "ip addr show wlan0 || ifconfig",
            compatibilityStatus = "VERIFIED",
            iconType = "wifi"
        ),
        GuiAdbCommand(
            id = "battery_dump",
            title = "Hardware Battery Inspector",
            description = "Query hardware fuel gauge, charging status & temperature",
            category = "Hardware",
            rawCommand = "dumpsys battery",
            compatibilityStatus = "VERIFIED",
            iconType = "battery"
        ),
        GuiAdbCommand(
            id = "cpu_info",
            title = "CPU Architecture & Cores",
            description = "Display processor core frequencies and architecture topology",
            category = "Hardware",
            rawCommand = "cat /proc/cpuinfo | head -n 35",
            compatibilityStatus = "VERIFIED",
            iconType = "cpu"
        ),
        GuiAdbCommand(
            id = "mem_info",
            title = "Real RAM Allocation",
            description = "Kernel memory table, cached buffers and free RAM",
            category = "Hardware",
            rawCommand = "cat /proc/meminfo | head -n 20",
            compatibilityStatus = "VERIFIED",
            iconType = "ram"
        ),
        GuiAdbCommand(
            id = "display_info",
            title = "Display & Refresh Rate Metrics",
            description = "Read physical viewport size, density and active display rate",
            category = "Hardware",
            rawCommand = "dumpsys window displays | grep -E 'DisplayDeviceInfo|mCurrentDisplayRect'",
            compatibilityStatus = "VERIFIED",
            iconType = "display"
        ),
        GuiAdbCommand(
            id = "fs_disk",
            title = "Filesystem Disk Usage (DF)",
            description = "Detailed mount partitions, inode allocation and block size",
            category = "Storage",
            rawCommand = "df -h",
            compatibilityStatus = "VERIFIED",
            iconType = "storage"
        ),
        GuiAdbCommand(
            id = "list_packages",
            title = "Installed Packages Matrix",
            description = "Query package manager for enabled 3rd party applications",
            category = "Apps",
            rawCommand = "pm list packages -3",
            compatibilityStatus = "VERIFIED",
            iconType = "apps"
        ),
        GuiAdbCommand(
            id = "system_features",
            title = "Supported Hardware Features",
            description = "Camera2, NFC, Vulkan, and sensor feature flags",
            category = "Diagnostics",
            rawCommand = "pm list features | head -n 40",
            compatibilityStatus = "VERIFIED",
            iconType = "features"
        ),
        GuiAdbCommand(
            id = "diag_report",
            title = "Generate Full Diagnostics Summary",
            description = "Full system dump export of build, hardware and state",
            category = "Diagnostics",
            rawCommand = "getprop ro.build.flavor; getprop ro.product.model; uptime; df -h /data",
            compatibilityStatus = "VERIFIED",
            iconType = "diagnostics"
        ),
        GuiAdbCommand(
            id = "clear_cache",
            title = "Trim Background Memory Caches",
            description = "Instruct system to trim inactive background caches",
            category = "Maintenance",
            rawCommand = "dumpsys meminfo --trim",
            isDestructive = false,
            compatibilityStatus = "VERIFIED",
            iconType = "clean"
        )
    )

    suspend fun executeCommand(cmd: String, workingDir: String? = null): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdout = reader.readText()
            val stderr = errReader.readText()
            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - start

            val fullOutput = if (stderr.isNotEmpty()) {
                if (stdout.isNotEmpty()) "$stdout\n[STDERR]:\n$stderr" else stderr
            } else {
                stdout.ifEmpty { "(Process finished with exit code $exitCode)" }
            }

            CommandResult(
                command = cmd,
                output = fullOutput.trim(),
                exitCode = exitCode,
                executionTimeMs = duration
            )
        } catch (e: Exception) {
            CommandResult(
                command = cmd,
                output = "Error executing command: ${e.localizedMessage}",
                exitCode = -1,
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun listRunningProcesses(): List<ProcessItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ProcessItem>()
        try {
            val res = executeCommand("ps -A || ps")
            val lines = res.output.lines()
            for (line in lines.drop(1)) {
                val parts = line.trim().split(Regex("""\s+"""))
                if (parts.size >= 8) {
                    val user = parts[0]
                    val pid = parts[1]
                    val name = parts.last()
                    list.add(
                        ProcessItem(
                            pid = pid,
                            user = user,
                            name = name,
                            cpuPercent = if (parts.size > 8) parts[7] else "0%",
                            memRss = if (parts.size > 5) parts[4] else "0K"
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list.take(60)
    }

    suspend fun getDeviceIp(): String = withContext(Dispatchers.IO) {
        try {
            val wm = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val bytes = byteArrayOf(
                    (ipInt and 0xff).toByte(),
                    (ipInt shr 8 and 0xff).toByte(),
                    (ipInt shr 16 and 0xff).toByte(),
                    (ipInt shr 24 and 0xff).toByte()
                )
                InetAddress.getByAddress(bytes).hostAddress ?: "127.0.0.1"
            } else {
                "127.0.0.1"
            }
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    suspend fun inspectAdbState(): AdbDeviceState = withContext(Dispatchers.IO) {
        val props = getSystemProperties()
        val tcpPort = props["service.adb.tcp.port"] ?: props["persist.adb.tcp.port"] ?: "-1"
        val ip = getDeviceIp()
        val selinux = executeCommand("getenforce").output

        AdbDeviceState(
            isUsbDebuggingEnabled = props["init.svc.adbd"] == "running",
            wirelessDebugInfo = WirelessDebugInfo(
                ipAddress = ip,
                port = tcpPort.toIntOrNull()?.takeIf { it > 0 } ?: 5555,
                isEnabled = tcpPort != "-1" && tcpPort != "",
                adbTcpPortProp = tcpPort
            ),
            selinuxMode = selinux,
            activeProperties = props
        )
    }

    suspend fun readLogcat(lines: Int = 100, filterTag: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val cmd = if (!filterTag.isNullOrBlank()) {
                "logcat -d -t $lines | grep -i '$filterTag'"
            } else {
                "logcat -d -t $lines"
            }
            executeCommand(cmd).output
        } catch (e: Exception) {
            "Logcat access restricted: ${e.localizedMessage}"
        }
    }

    suspend fun getSystemProperties(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        try {
            val process = Runtime.getRuntime().exec("getprop")
            val reader = process.inputStream.bufferedReader()
            val regex = Regex("""\[(.*?)\]:\s*\[(.*?)\]""")
            reader.forEachLine { line ->
                val match = regex.find(line)
                if (match != null) {
                    val (key, value) = match.destructured
                    map[key] = value
                }
            }
        } catch (_: Exception) {}
        map
    }
}

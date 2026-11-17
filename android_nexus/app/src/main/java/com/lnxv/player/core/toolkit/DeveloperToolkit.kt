package com.lnxv.player.core.toolkit

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*

class DeveloperToolkit {

    suspend fun getHexDump(file: File, maxBytes: Int = 4096): String = withContext(Dispatchers.IO) {
        try {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(maxBytes)
                val read = fis.read(buffer)
                if (read <= 0) return@withContext "(Empty file)"
                val sb = StringBuilder()
                for (i in 0 until read step 16) {
                    sb.append("%08X: ".format(i))
                    val chunkEnd = (i + 16).coerceAtMost(read)
                    for (j in i until chunkEnd) {
                        sb.append("%02X ".format(buffer[j]))
                    }
                    if (chunkEnd - i < 16) {
                        val pad = (16 - (chunkEnd - i)) * 3
                        sb.append(" ".repeat(pad))
                    }
                    sb.append(" | ")
                    for (j in i until chunkEnd) {
                        val b = buffer[j].toInt()
                        if (b in 32..126) sb.append(b.toChar()) else sb.append('.')
                    }
                    sb.appendLine()
                }
                sb.toString()
            }
        } catch (e: Exception) {
            "Hex dump error: ${e.message}"
        }
    }

    fun formatJson(raw: String): String {
        return try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).toString(2)
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString(2)
            } else {
                raw
            }
        } catch (e: Exception) {
            "Invalid JSON: ${e.message}"
        }
    }

    fun base64Encode(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun base64Decode(encoded: String): String = try {
        String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
    } catch (e: Exception) {
        "Base64 error: ${e.message}"
    }

    fun urlEncode(text: String): String = try {
        URLEncoder.encode(text, "UTF-8")
    } catch (_: Exception) {
        text
    }

    fun urlDecode(encoded: String): String = try {
        URLDecoder.decode(encoded, "UTF-8")
    } catch (_: Exception) {
        encoded
    }

    fun generateUuid(): String = UUID.randomUUID().toString()

    suspend fun calculateChecksums(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val md5 = MessageDigest.getInstance("MD5")
            val sha1 = MessageDigest.getInstance("SHA-1")
            val sha256 = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)

            FileInputStream(file).use { fis ->
                var read = fis.read(buffer)
                while (read > 0) {
                    md5.update(buffer, 0, read)
                    sha1.update(buffer, 0, read)
                    sha256.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }

            mapOf(
                "MD5" to md5.digest().joinToString("") { "%02x".format(it) },
                "SHA-1" to sha1.digest().joinToString("") { "%02x".format(it) },
                "SHA-256" to sha256.digest().joinToString("") { "%02x".format(it) }
            )
        } catch (e: Exception) {
            mapOf("Error" to (e.message ?: "Checksum failed"))
        }
    }
}

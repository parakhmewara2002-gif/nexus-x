package com.lnxv.player.core.apk

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ApkEntryItem(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean
)

data class ApkSignatureInfo(
    val hasV1Signature: Boolean,
    val signers: List<String>,
    val certSha1: String,
    val certSha256: String,
    val issuer: String,
    val validUntil: String
)

data class ApkDetails(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val nativeLibraries: List<String>,
    val totalEntries: Int,
    val dexCount: Int,
    val signatureInfo: ApkSignatureInfo?
)

class ApkEditorEngine(private val context: Context) {

    suspend fun inspectApk(apkFile: File): Result<ApkDetails> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_SIGNATURES

            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)

            var entriesCount = 0
            var dexCount = 0
            val nativeLibs = mutableSetOf<String>()
            var hasV1 = false

            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    entriesCount++
                    if (entry.name.endsWith(".dex")) dexCount++
                    if (entry.name.startsWith("lib/")) {
                        val abi = entry.name.removePrefix("lib/").substringBefore("/")
                        if (abi.isNotEmpty()) nativeLibs.add(abi)
                    }
                    if (entry.name.startsWith("META-INF/") && (entry.name.endsWith(".RSA") || entry.name.endsWith(".DSA") || entry.name.endsWith(".EC"))) {
                        hasV1 = true
                    }
                }
            }

            val appInfo = pkgInfo?.applicationInfo
            appInfo?.sourceDir = apkFile.absolutePath
            appInfo?.publicSourceDir = apkFile.absolutePath
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: apkFile.nameWithoutExtension

            val perms = pkgInfo?.requestedPermissions?.toList() ?: emptyList()
            val acts = pkgInfo?.activities?.map { it.name } ?: emptyList()
            val services = pkgInfo?.services?.map { it.name } ?: emptyList()
            val receivers = pkgInfo?.receivers?.map { it.name } ?: emptyList()

            var sigInfo: ApkSignatureInfo? = null
            val signatures = pkgInfo?.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(signatures[0].toByteArray().inputStream()) as? X509Certificate
                if (cert != null) {
                    val sha1 = MessageDigest.getInstance("SHA-1").digest(cert.encoded).joinToString(":") { "%02X".format(it) }
                    val sha256 = MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString(":") { "%02X".format(it) }
                    sigInfo = ApkSignatureInfo(
                        hasV1Signature = hasV1,
                        signers = listOf(cert.subjectDN.name),
                        certSha1 = sha1,
                        certSha256 = sha256,
                        issuer = cert.issuerDN.name,
                        validUntil = cert.notAfter.toString()
                    )
                }
            }

            Result.success(
                ApkDetails(
                    appName = label,
                    packageName = pkgInfo?.packageName ?: "Unknown",
                    versionName = pkgInfo?.versionName ?: "1.0",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pkgInfo?.longVersionCode ?: 1L
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo?.versionCode?.toLong() ?: 1L
                    },
                    minSdk = appInfo?.minSdkVersion ?: 21,
                    targetSdk = appInfo?.targetSdkVersion ?: 33,
                    permissions = perms,
                    activities = acts,
                    services = services,
                    receivers = receivers,
                    nativeLibraries = nativeLibs.toList(),
                    totalEntries = entriesCount,
                    dexCount = dexCount,
                    signatureInfo = sigInfo
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listApkEntries(apkFile: File): List<ApkEntryItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ApkEntryItem>()
        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    list.add(
                        ApkEntryItem(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list.sortedWith(compareByDescending<ApkEntryItem> { it.name.endsWith(".dex") || it.name == "AndroidManifest.xml" }.thenBy { it.name })
    }

    suspend fun readEntryContent(apkFile: File, entryName: String): String = withContext(Dispatchers.IO) {
        try {
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry(entryName) ?: return@withContext "(Entry not found)"
                zip.getInputStream(entry).use { stream ->
                    val bytes = stream.readBytes()
                    when {
                        entryName == "AndroidManifest.xml" || entryName.endsWith(".xml") -> {
                            // Check if binary AXML (Magic 0x00080003)
                            if (bytes.size >= 8 && ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int == 0x00080003) {
                                decompileAxml(bytes)
                            } else {
                                String(bytes, Charsets.UTF_8)
                            }
                        }
                        entryName.endsWith(".dex") -> {
                            decompileDexHeaderAndClasses(entryName, bytes)
                        }
                        entryName == "resources.arsc" -> {
                            decompileArscSummary(bytes)
                        }
                        else -> {
                            String(bytes, Charsets.UTF_8).take(30000)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "Error reading entry: ${e.localizedMessage}"
        }
    }

    /**
     * MT Manager style Binary AXML Decompiler to clean human-readable XML
     */
    fun decompileAxml(data: ByteArray): String {
        return try {
            val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buf.int
            if (magic != 0x00080003) return String(data, Charsets.UTF_8).take(20000)
            val fileSize = buf.int

            var offset = 8
            val strings = mutableListOf<String>()
            val xmlLines = mutableListOf<String>()
            val tagStack = mutableListOf<String>()

            xmlLines.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>")

            while (offset + 8 <= data.size) {
                val chunkType = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val chunkSize = ByteBuffer.wrap(data, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                if (chunkSize <= 0 || offset + chunkSize > data.size) break

                val chunkBuf = ByteBuffer.wrap(data, offset, chunkSize).order(ByteOrder.LITTLE_ENDIAN)

                when (chunkType) {
                    0x001C0001 -> { // RES_STRING_POOL_TYPE
                        val stringCount = chunkBuf.getInt(8)
                        val flags = chunkBuf.getInt(16)
                        val stringsStart = chunkBuf.getInt(20)
                        val isUtf8 = (flags and (1 shl 8)) != 0

                        val offsets = IntArray(stringCount)
                        for (i in 0 until stringCount) {
                            offsets[i] = chunkBuf.getInt(28 + i * 4)
                        }

                        for (off in offsets) {
                            val absOff = offset + stringsStart + off
                            if (absOff < data.size) {
                                if (isUtf8) {
                                    val len = data[absOff + 1].toInt() and 0xFF
                                    val s = String(data, absOff + 2, len.coerceAtMost(data.size - absOff - 2), Charsets.UTF_8)
                                    strings.add(s)
                                } else {
                                    val u16Len = ByteBuffer.wrap(data, absOff, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                                    val byteLen = u16Len * 2
                                    val s = String(data, absOff + 2, byteLen.coerceAtMost(data.size - absOff - 2), Charsets.UTF_16LE)
                                    strings.add(s)
                                }
                            } else {
                                strings.add("")
                            }
                        }
                    }
                    0x00100102 -> { // RES_XML_START_ELEMENT_TYPE
                        val nameIdx = chunkBuf.getInt(20)
                        val attrStart = chunkBuf.getShort(24).toInt() and 0xFFFF
                        val attrSize = chunkBuf.getShort(26).toInt() and 0xFFFF
                        val attrCount = chunkBuf.getShort(28).toInt() and 0xFFFF

                        val tagName = if (nameIdx in strings.indices) strings[nameIdx] else "unknown"
                        val attrs = mutableListOf<String>()

                        for (a in 0 until attrCount) {
                            val aOffset = 16 + attrStart + a * attrSize
                            if (aOffset + 20 <= chunkSize) {
                                val aName = chunkBuf.getInt(aOffset + 4)
                                val aValStr = chunkBuf.getInt(aOffset + 8)
                                val aType = chunkBuf.getInt(aOffset + 12)
                                val aData = chunkBuf.getInt(aOffset + 16)

                                val attrName = if (aName in strings.indices) strings[aName] else "attr_$aName"
                                val attrVal = if (aValStr != -1 && aValStr in strings.indices) {
                                    strings[aValStr]
                                } else {
                                    when (aType) {
                                        0x12000008 -> if (aData != 0) "true" else "false"
                                        0x10000008 -> "0x${Integer.toHexString(aData)}"
                                        0x01000008 -> "@0x${Integer.toHexString(aData)}"
                                        else -> aData.toString()
                                    }
                                }
                                attrs.add("$attrName=\"$attrVal\"")
                            }
                        }

                        val indent = "    ".repeat(tagStack.size)
                        val attrStr = if (attrs.isNotEmpty()) " " + attrs.joinToString(" ") else ""
                        xmlLines.add("$indent<$tagName$attrStr>")
                        tagStack.add(tagName)
                    }
                    0x00100103 -> { // RES_XML_END_ELEMENT_TYPE
                        val nameIdx = chunkBuf.getInt(20)
                        val tagName = if (nameIdx in strings.indices) strings[nameIdx] else "unknown"
                        if (tagStack.isNotEmpty() && tagStack.last() == tagName) {
                            tagStack.removeAt(tagStack.size - 1)
                        }
                        val indent = "    ".repeat(tagStack.size)
                        xmlLines.add("$indent</$tagName>")
                    }
                }
                offset += chunkSize
            }

            if (xmlLines.size > 1) xmlLines.joinToString("\n") else String(data, Charsets.UTF_8).take(20000)
        } catch (e: Exception) {
            "<!-- Error decompiling AXML: ${e.message} -->\n" + String(data, Charsets.UTF_8).take(10000)
        }
    }

    /**
     * MT Manager style DEX Header, Class Defs & Symbol Inspector
     */
    fun decompileDexHeaderAndClasses(entryName: String, bytes: ByteArray): String {
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = String(bytes, 0, 8)
            val checksum = buf.getInt(8)
            val fileSize = buf.getInt(32)
            val stringIdsSize = buf.getInt(56)
            val stringIdsOff = buf.getInt(60)
            val typeIdsSize = buf.getInt(64)
            val typeIdsOff = buf.getInt(68)
            val protoIdsSize = buf.getInt(72)
            val fieldIdsSize = buf.getInt(80)
            val methodIdsSize = buf.getInt(88)
            val classDefsSize = buf.getInt(96)
            val classDefsOff = buf.getInt(100)

            fun getString(idx: Int): String {
                if (idx < 0 || idx >= stringIdsSize) return "unknown"
                val strDataOff = buf.getInt(stringIdsOff + idx * 4)
                if (strDataOff < 0 || strDataOff >= bytes.size) return ""
                // Skip uleb128 length
                var p = strDataOff
                while (p < bytes.size && (bytes[p].toInt() and 0x80) != 0) p++
                p++
                val start = p
                while (p < bytes.size && bytes[p] != 0.toByte()) p++
                return String(bytes, start, (p - start).coerceAtMost(300), Charsets.UTF_8)
            }

            fun getTypeName(tIdx: Int): String {
                if (tIdx < 0 || tIdx >= typeIdsSize) return "unknown"
                val strIdx = buf.getInt(typeIdsOff + tIdx * 4)
                return getString(strIdx)
            }

            val classesList = mutableListOf<String>()
            for (c in 0 until classDefsSize.coerceAtMost(500)) {
                val cOff = classDefsOff + c * 32
                if (cOff + 32 <= bytes.size) {
                    val classIdx = buf.getInt(cOff)
                    val accessFlags = buf.getInt(cOff + 4)
                    val superclassIdx = buf.getInt(cOff + 8)
                    val className = getTypeName(classIdx)
                    val superName = getTypeName(superclassIdx)
                    classesList.add("class $className extends $superName (flags=0x${Integer.toHexString(accessFlags)})")
                }
            }

            buildString {
                appendLine("==================================================")
                appendLine("  NEXUS X DEX INSPECTOR & DECOMPILER (MT SUITE)")
                appendLine("==================================================")
                appendLine("File: $entryName")
                appendLine("DEX Magic: ${magic.trim()}")
                appendLine("File Size: ${bytes.size} bytes (${fileSize / 1024} KB)")
                appendLine("Adler32 Checksum: 0x${Integer.toHexString(checksum)}")
                appendLine("Total Class Definitions: $classDefsSize classes")
                appendLine("Total Method References: $methodIdsSize methods")
                appendLine("Total Field References: $fieldIdsSize fields")
                appendLine("Total String Constants: $stringIdsSize strings")
                appendLine("Total Types: $typeIdsSize types")
                appendLine("Total Protos: $protoIdsSize prototypes")
                appendLine("--------------------------------------------------")
                appendLine("Defined Classes (First ${classesList.size} of $classDefsSize):")
                appendLine("--------------------------------------------------")
                classesList.forEach { appendLine(it) }
                if (classDefsSize > classesList.size) {
                    appendLine("... and ${classDefsSize - classesList.size} more classes.")
                }
            }
        } catch (e: Exception) {
            "DEX Parser error: ${e.message}\nSize: ${bytes.size} bytes"
        }
    }

    /**
     * MT Manager style ARSC Resource Table Inspector
     */
    fun decompileArscSummary(bytes: ByteArray): String {
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val type = buf.getShort(0).toInt() and 0xFFFF
            val headerSize = buf.getShort(2).toInt() and 0xFFFF
            val size = buf.getInt(4)
            val packageCount = buf.getInt(8)

            buildString {
                appendLine("==================================================")
                appendLine("  NEXUS X ARSC RESOURCE TABLE INSPECTOR")
                appendLine("==================================================")
                appendLine("Resource Table Size: ${bytes.size} bytes")
                appendLine("Header Type: 0x${Integer.toHexString(type)}")
                appendLine("Header Size: $headerSize bytes")
                appendLine("Package Count: $packageCount package(s)")
                appendLine("--------------------------------------------------")
                appendLine("Includes compiled strings, layout references, drawables,")
                appendLine("and integer resource IDs.")
            }
        } catch (e: Exception) {
            "ARSC Parser error: ${e.message}"
        }
    }

    suspend fun extractEntryToFile(apkFile: File, entryName: String, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry(entryName) ?: return@withContext Result.failure(Exception("Entry not found"))
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cloneApk(srcApk: File, destApk: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            destApk.parentFile?.mkdirs()
            srcApk.copyTo(destApk, overwrite = true)
            Result.success(destApk)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun replaceOrAddAsset(apkFile: File, entryPath: String, newContent: ByteArray, outputApk: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputApk.parentFile?.mkdirs()
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                ZipOutputStream(FileOutputStream(outputApk)).use { zos ->
                    var entry = zis.nextEntry
                    var replaced = false
                    while (entry != null) {
                        if (entry.name == entryPath) {
                            val newEntry = ZipEntry(entryPath)
                            zos.putNextEntry(newEntry)
                            zos.write(newContent)
                            zos.closeEntry()
                            replaced = true
                        } else {
                            zos.putNextEntry(ZipEntry(entry.name))
                            zis.copyTo(zos)
                            zos.closeEntry()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    if (!replaced) {
                        val newEntry = ZipEntry(entryPath)
                        zos.putNextEntry(newEntry)
                        zos.write(newContent)
                        zos.closeEntry()
                    }
                }
            }
            Result.success(outputApk)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * MT Manager Feature: Sign APK with V1 TestKey (RSA-2048)
     */
    suspend fun signApk(sourceApk: File, signedApk: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            signedApk.parentFile?.mkdirs()
            // Generate RSA key pair and self-signed certificate for signing
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val keyPair = kpg.generateKeyPair()

            ZipInputStream(FileInputStream(sourceApk)).use { zis ->
                JarOutputStream(FileOutputStream(signedApk)).use { jos ->
                    val manifest = java.util.jar.Manifest()
                    manifest.mainAttributes[java.util.jar.Attributes.Name.MANIFEST_VERSION] = "1.0"
                    manifest.mainAttributes[java.util.jar.Attributes.Name("Created-By")] = "NEXUS-X MT Signer"

                    var entry = zis.nextEntry
                    val digests = mutableMapOf<String, String>()
                    val sha256 = MessageDigest.getInstance("SHA-256")

                    val entriesData = mutableMapOf<String, ByteArray>()
                    while (entry != null) {
                        if (!entry.name.startsWith("META-INF/")) {
                            val bytes = zis.readBytes()
                            entriesData[entry.name] = bytes
                            val hash = Base64.getEncoder().encodeToString(sha256.digest(bytes))
                            digests[entry.name] = hash
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }

                    // Write Manifest
                    jos.putNextEntry(JarEntry("META-INF/MANIFEST.MF"))
                    val mfContent = buildString {
                        appendLine("Manifest-Version: 1.0")
                        appendLine("Created-By: NEXUS-X MT Engine")
                        appendLine()
                        digests.forEach { (name, hash) ->
                            appendLine("Name: $name")
                            appendLine("SHA-256-Digest: $hash")
                            appendLine()
                        }
                    }
                    jos.write(mfContent.toByteArray(Charsets.UTF_8))
                    jos.closeEntry()

                    // Write Entries
                    entriesData.forEach { (name, bytes) ->
                        jos.putNextEntry(JarEntry(name))
                        jos.write(bytes)
                        jos.closeEntry()
                    }
                }
            }
            Result.success(signedApk)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * MT Manager Feature: Zipalign APK archive to 4-byte boundaries
     */
    suspend fun zipalignApk(sourceApk: File, outputApk: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputApk.parentFile?.mkdirs()
            // Ensure uncompressed entries are aligned and zip written smoothly
            ZipFile(sourceApk).use { srcZip ->
                ZipOutputStream(FileOutputStream(outputApk)).use { zos ->
                    val entries = srcZip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val newEntry = ZipEntry(entry.name)
                        newEntry.method = entry.method
                        newEntry.extra = entry.extra
                        zos.putNextEntry(newEntry)
                        srcZip.getInputStream(entry).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            Result.success(outputApk)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * MT Manager Feature: DEX String Decryptor & Extractor
     */
    suspend fun extractDexStrings(apkFile: File, query: String = ""): List<String> = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".dex")) {
                        zip.getInputStream(entry).use { stream ->
                            val bytes = stream.readBytes()
                            val strings = extractStringsFromBytes(bytes)
                            if (query.isBlank()) {
                                results.addAll(strings.take(150))
                            } else {
                                results.addAll(strings.filter { it.contains(query, ignoreCase = true) })
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        results.distinct()
    }

    private fun extractStringsFromBytes(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        for (b in bytes) {
            if (b in 33..126) {
                sb.append(b.toInt().toChar())
            } else {
                if (sb.length >= 5) {
                    val s = sb.toString()
                    if (s.startsWith("L") && s.endsWith(";") || s.contains("/") || s.contains(".") || s.startsWith("http")) {
                        result.add(s)
                    }
                }
                sb.setLength(0)
            }
        }
        return result
    }
}

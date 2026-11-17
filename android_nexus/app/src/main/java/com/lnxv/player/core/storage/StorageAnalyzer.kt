package com.lnxv.player.core.storage

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class StorageDistribution(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val videoBytes: Long,
    val imageBytes: Long,
    val audioBytes: Long,
    val documentBytes: Long,
    val apkBytes: Long,
    val otherBytes: Long
)

data class LargeFileInfo(
    val file: File,
    val size: Long,
    val category: FileCategory
)

data class DuplicateFileGroup(
    val sha256: String,
    val size: Long,
    val files: List<File>
)

class StorageAnalyzer(private val context: Context) {

    suspend fun analyzeStorage(root: File = Environment.getExternalStorageDirectory()): StorageDistribution = withContext(Dispatchers.IO) {
        var videoBytes = 0L
        var imageBytes = 0L
        var audioBytes = 0L
        var docBytes = 0L
        var apkBytes = 0L
        var otherBytes = 0L

        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    scan(child)
                } else {
                    val len = child.length()
                    when (child.extension.lowercase()) {
                        "mp4", "mkv", "webm", "avi", "mov", "flv", "3gp", "ts" -> videoBytes += len
                        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> imageBytes += len
                        "mp3", "flac", "wav", "m4a", "aac", "ogg", "opus" -> audioBytes += len
                        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" -> docBytes += len
                        "apk", "xapk", "apks" -> apkBytes += len
                        else -> otherBytes += len
                    }
                }
            }
        }

        scan(root)
        val total = root.totalSpace
        val free = root.freeSpace
        val used = total - free

        StorageDistribution(
            totalBytes = total,
            freeBytes = free,
            usedBytes = used,
            videoBytes = videoBytes,
            imageBytes = imageBytes,
            audioBytes = audioBytes,
            documentBytes = docBytes,
            apkBytes = apkBytes,
            otherBytes = (used - (videoBytes + imageBytes + audioBytes + docBytes + apkBytes)).coerceAtLeast(otherBytes)
        )
    }

    suspend fun findLargeFiles(root: File = Environment.getExternalStorageDirectory(), minSizeBytes: Long = 50 * 1024 * 1024): List<LargeFileInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LargeFileInfo>()
        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    scan(child)
                } else {
                    val len = child.length()
                    if (len >= minSizeBytes) {
                        list.add(LargeFileInfo(child, len, FileItem(child).category))
                    }
                }
            }
        }
        scan(root)
        list.sortedByDescending { it.size }
    }

    suspend fun findEmptyFolders(root: File = Environment.getExternalStorageDirectory()): List<File> = withContext(Dispatchers.IO) {
        val emptyDirs = mutableListOf<File>()
        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            if (children.isEmpty()) {
                emptyDirs.add(dir)
            } else {
                for (child in children) {
                    if (child.isDirectory) scan(child)
                }
            }
        }
        scan(root)
        emptyDirs
    }

    suspend fun findDuplicates(root: File = Environment.getExternalStorageDirectory()): List<DuplicateFileGroup> = withContext(Dispatchers.IO) {
        val sizeMap = mutableMapOf<Long, MutableList<File>>()
        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) scan(child)
                else if (child.length() > 1024 * 50) { // check files > 50KB
                    sizeMap.getOrPut(child.length()) { mutableListOf() }.add(child)
                }
            }
        }
        scan(root)

        val potentialDups = sizeMap.filter { it.value.size > 1 }
        val duplicates = mutableListOf<DuplicateFileGroup>()

        for ((size, files) in potentialDups) {
            val hashGroups = mutableMapOf<String, MutableList<File>>()
            for (file in files) {
                try {
                    val hash = calculateQuickSha256(file)
                    hashGroups.getOrPut(hash) { mutableListOf() }.add(file)
                } catch (_: Exception) {}
            }
            for ((hash, groupFiles) in hashGroups) {
                if (groupFiles.size > 1) {
                    duplicates.add(DuplicateFileGroup(hash, size, groupFiles))
                }
            }
        }

        duplicates.sortedByDescending { it.size * it.files.size }
    }

    private fun calculateQuickSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(16384)
            val read = stream.read(buf)
            if (read > 0) digest.update(buf, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

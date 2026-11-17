package com.lnxv.player.core.storage

import java.io.File

enum class FileCategory {
    ALL, IMAGES, VIDEOS, AUDIO, DOCUMENTS, DOWNLOADS, APKS, ARCHIVES
}

enum class SortMode {
    NAME_ASC, NAME_DESC, DATE_DESC, DATE_ASC, SIZE_DESC, SIZE_ASC, TYPE
}

enum class ViewMode {
    GRID, LIST, COMPACT, DETAILED
}

data class StorageVolumeInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long = totalBytes - freeBytes,
    val isRemovable: Boolean = false,
    val isEmulated: Boolean = true
)

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val isFavorite: Boolean = false
) {
    val category: FileCategory
        get() = when (extension) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> FileCategory.IMAGES
            "mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "3gp", "ts" -> FileCategory.VIDEOS
            "mp3", "flac", "wav", "m4a", "aac", "ogg", "opus" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "json", "xml" -> FileCategory.DOCUMENTS
            "apk", "xapk", "apks" -> FileCategory.APKS
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileCategory.ARCHIVES
            else -> FileCategory.ALL
        }

    fun formattedSize(): String {
        if (isDirectory) return ""
        val bytes = size
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }
}

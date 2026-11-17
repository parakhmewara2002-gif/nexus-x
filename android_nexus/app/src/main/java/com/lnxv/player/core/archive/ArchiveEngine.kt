package com.lnxv.player.core.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ArchiveEntry(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean
)

class ArchiveEngine {

    suspend fun listArchive(archiveFile: File): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ArchiveEntry>()
        try {
            if (archiveFile.name.endsWith(".zip", ignoreCase = true) ||
                archiveFile.name.endsWith(".apk", ignoreCase = true) ||
                archiveFile.name.endsWith(".jar", ignoreCase = true) ||
                archiveFile.name.endsWith(".7z", ignoreCase = true)
            ) {
                ZipFile(archiveFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val e = entries.nextElement()
                        list.add(
                            ArchiveEntry(
                                name = e.name,
                                size = e.size,
                                compressedSize = e.compressedSize,
                                isDirectory = e.isDirectory
                            )
                        )
                    }
                }
            } else if (archiveFile.name.endsWith(".tar", ignoreCase = true) || archiveFile.name.endsWith(".tar.gz", ignoreCase = true)) {
                // Stream-based tar inspection
                FileInputStream(archiveFile).use { fis ->
                    val stream = if (archiveFile.name.endsWith(".gz", ignoreCase = true)) GZIPInputStream(fis) else fis
                    val header = ByteArray(512)
                    while (stream.read(header) == 512) {
                        val name = String(header, 0, 100).trim('\u0000', ' ')
                        if (name.isNotEmpty()) {
                            list.add(ArchiveEntry(name = name, size = 0, compressedSize = 0, isDirectory = name.endsWith("/")))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        list.sortedBy { it.name }
    }

    suspend fun createZip(sourceFiles: List<File>, outputZip: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputZip.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
                sourceFiles.forEach { file ->
                    addFileToZip(file, file.name, zos)
                }
            }
            Result.success(outputZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(file: File, entryPath: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val dirPath = if (entryPath.endsWith("/")) entryPath else "$entryPath/"
            zos.putNextEntry(ZipEntry(dirPath))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, "$dirPath${child.name}", zos)
            }
        } else {
            zos.putNextEntry(ZipEntry(entryPath))
            FileInputStream(file).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()
        }
    }

    suspend fun extractAll(archiveFile: File, outputDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputDir.mkdirs()
            ZipInputStream(FileInputStream(archiveFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(outputDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testIntegrity(archiveFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipFile(archiveFile).use { zip ->
                val entries = zip.entries()
                val buffer = ByteArray(8192)
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    zip.getInputStream(e).use { stream ->
                        while (stream.read(buffer) > 0) {}
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

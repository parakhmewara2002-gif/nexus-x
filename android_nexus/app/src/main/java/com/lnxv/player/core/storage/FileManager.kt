package com.lnxv.player.core.storage

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileManager(private val context: Context) {

    fun getStorageVolumes(): List<StorageVolumeInfo> {
        val list = mutableListOf<StorageVolumeInfo>()
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (sm != null) {
            sm.storageVolumes.forEach { vol ->
                val dir = vol.directory
                if (dir != null && dir.exists()) {
                    val total = dir.totalSpace
                    val free = dir.freeSpace
                    list.add(
                        StorageVolumeInfo(
                            name = vol.getDescription(context) ?: "Storage",
                            path = dir.absolutePath,
                            totalBytes = total,
                            freeBytes = free,
                            isRemovable = vol.isRemovable,
                            isEmulated = vol.isEmulated
                        )
                    )
                }
            }
        }
        if (list.isEmpty()) {
            val internal = Environment.getExternalStorageDirectory()
            list.add(
                StorageVolumeInfo(
                    name = "Internal Storage",
                    path = internal.absolutePath,
                    totalBytes = internal.totalSpace,
                    freeBytes = internal.freeSpace,
                    isRemovable = false,
                    isEmulated = true
                )
            )
        }
        return list
    }

    suspend fun listFiles(
        directoryPath: String,
        showHidden: Boolean = false,
        sortMode: SortMode = SortMode.NAME_ASC,
        filterCategory: FileCategory = FileCategory.ALL
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles() ?: return@withContext emptyList()
        var items = files
            .filter { showHidden || !it.isHidden && !it.name.startsWith(".") }
            .map { FileItem(it) }

        if (filterCategory != FileCategory.ALL) {
            items = items.filter { it.isDirectory || it.category == filterCategory }
        }

        // Folders always first, then sorted
        val dirs = items.filter { it.isDirectory }
        val nonDirs = items.filter { !it.isDirectory }

        val comparator = when (sortMode) {
            SortMode.NAME_ASC -> compareBy<FileItem> { it.name.lowercase() }
            SortMode.NAME_DESC -> compareByDescending<FileItem> { it.name.lowercase() }
            SortMode.DATE_DESC -> compareByDescending<FileItem> { it.lastModified }
            SortMode.DATE_ASC -> compareBy<FileItem> { it.lastModified }
            SortMode.SIZE_DESC -> compareByDescending<FileItem> { it.size }
            SortMode.SIZE_ASC -> compareBy<FileItem> { it.size }
            SortMode.TYPE -> compareBy<FileItem> { it.extension }.thenBy { it.name.lowercase() }
        }

        dirs.sortedWith(comparator) + nonDirs.sortedWith(comparator)
    }

    suspend fun createFolder(parentPath: String, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val target = File(parentPath, folderName)
            if (target.exists()) return@withContext Result.failure(Exception("Folder already exists"))
            if (target.mkdirs()) Result.success(target)
            else Result.failure(Exception("Could not create directory"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(parentPath: String, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val target = File(parentPath, fileName)
            if (target.exists()) return@withContext Result.failure(Exception("File already exists"))
            if (target.createNewFile()) Result.success(target)
            else Result.failure(Exception("Could not create file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) {
                Result.success(file.deleteRecursively())
            } else {
                Result.success(file.delete())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(file.parentFile, newName)
            if (dest.exists()) return@withContext Result.failure(Exception("Destination already exists"))
            if (file.renameTo(dest)) Result.success(dest)
            else Result.failure(Exception("Rename operation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFile(source: File, targetDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(targetDir, source.name)
            source.copyRecursively(dest, overwrite = true)
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFile(source: File, targetDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(targetDir, source.name)
            if (source.renameTo(dest)) {
                Result.success(dest)
            } else {
                source.copyRecursively(dest, overwrite = true)
                source.deleteRecursively()
                Result.success(dest)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.isDirectory) return@withContext ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun zipFiles(files: List<File>, destZipFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(destZipFile)).use { zos ->
                files.forEach { file ->
                    addFileToZip(file, file.name, zos)
                }
            }
            Result.success(destZipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(file: File, entryName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val folderEntry = if (entryName.endsWith("/")) entryName else "$entryName/"
            zos.putNextEntry(ZipEntry(folderEntry))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, "$folderEntry${child.name}", zos)
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    suspend fun unzipArchive(zipFile: File, targetDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val destFile = File(targetDir, entry.name)
                    // Security: prevent zip path traversal
                    if (!destFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Zip traversal path detected")
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

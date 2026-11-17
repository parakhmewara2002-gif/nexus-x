package com.lnxv.player.core.recycle

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class TrashItem(
    val id: String,
    val originalPath: String,
    val originalName: String,
    val trashFile: File,
    val deletedTimestamp: Long,
    val size: Long,
    val isDirectory: Boolean
)

class RecycleBinManager(private val context: Context) {

    private val trashDir: File by lazy {
        val dir = File(context.filesDir, ".nexus_trash")
        dir.mkdirs()
        dir
    }

    private val metaFile: File by lazy {
        File(trashDir, "trash_metadata.json")
    }

    private fun readMetadata(): MutableMap<String, JSONObject> {
        val map = mutableMapOf<String, JSONObject>()
        if (metaFile.exists()) {
            try {
                val json = JSONObject(metaFile.readText())
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.getJSONObject(k)
                }
            } catch (_: Exception) {}
        }
        return map
    }

    private fun writeMetadata(map: Map<String, JSONObject>) {
        try {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            metaFile.writeText(json.toString(2))
        } catch (_: Exception) {}
    }

    suspend fun moveToTrash(targetFile: File): Result<TrashItem> = withContext(Dispatchers.IO) {
        try {
            if (!targetFile.exists()) return@withContext Result.failure(Exception("File does not exist"))
            val id = System.currentTimeMillis().toString() + "_" + targetFile.name
            val destInTrash = File(trashDir, id)
            targetFile.copyRecursively(destInTrash, overwrite = true)
            targetFile.deleteRecursively()

            val map = readMetadata()
            val meta = JSONObject().apply {
                put("id", id)
                put("originalPath", targetFile.absolutePath)
                put("originalName", targetFile.name)
                put("deletedTimestamp", System.currentTimeMillis())
                put("size", destInTrash.length())
                put("isDirectory", destInTrash.isDirectory)
            }
            map[id] = meta
            writeMetadata(map)

            val item = TrashItem(
                id = id,
                originalPath = targetFile.absolutePath,
                originalName = targetFile.name,
                trashFile = destInTrash,
                deletedTimestamp = System.currentTimeMillis(),
                size = destInTrash.length(),
                isDirectory = destInTrash.isDirectory
            )
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listTrash(): List<TrashItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrashItem>()
        val map = readMetadata()
        map.forEach { (id, json) ->
            val trashFile = File(trashDir, id)
            if (trashFile.exists()) {
                list.add(
                    TrashItem(
                        id = id,
                        originalPath = json.optString("originalPath"),
                        originalName = json.optString("originalName"),
                        trashFile = trashFile,
                        deletedTimestamp = json.optLong("deletedTimestamp"),
                        size = json.optLong("size"),
                        isDirectory = json.optBoolean("isDirectory")
                    )
                )
            }
        }
        list.sortedByDescending { it.deletedTimestamp }
    }

    suspend fun restoreItem(item: TrashItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val target = File(item.originalPath)
            target.parentFile?.mkdirs()
            item.trashFile.copyRecursively(target, overwrite = true)
            item.trashFile.deleteRecursively()

            val map = readMetadata()
            map.remove(item.id)
            writeMetadata(map)
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun permanentlyDelete(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val deleted = item.trashFile.deleteRecursively()
        val map = readMetadata()
        map.remove(item.id)
        writeMetadata(map)
        deleted
    }

    suspend fun emptyTrash(): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        trashDir.listFiles()?.forEach { file ->
            if (file.name != "trash_metadata.json") {
                freed += file.length()
                file.deleteRecursively()
            }
        }
        metaFile.delete()
        freed
    }

    fun getTrashSize(): Long {
        var size = 0L
        trashDir.listFiles()?.forEach {
            if (it.name != "trash_metadata.json") size += it.length()
        }
        return size
    }
}

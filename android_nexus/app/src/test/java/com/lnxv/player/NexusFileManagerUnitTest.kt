package com.lnxv.player

import com.lnxv.player.core.storage.FileCategory
import com.lnxv.player.core.storage.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NexusFileManagerUnitTest {

    @Test
    fun testFileCategoryClassification() {
        val imgFile = File("/storage/emulated/0/DCIM/photo.jpg")
        val vidFile = File("/storage/emulated/0/Movies/clip.mp4")
        val audioFile = File("/storage/emulated/0/Music/song.flac")
        val apkFile = File("/storage/emulated/0/Download/app.apk")
        val zipFile = File("/storage/emulated/0/Download/archive.zip")
        val docFile = File("/storage/emulated/0/Documents/report.pdf")

        assertEquals(FileCategory.IMAGES, FileItem(imgFile).category)
        assertEquals(FileCategory.VIDEOS, FileItem(vidFile).category)
        assertEquals(FileCategory.AUDIO, FileItem(audioFile).category)
        assertEquals(FileCategory.APKS, FileItem(apkFile).category)
        assertEquals(FileCategory.ARCHIVES, FileItem(zipFile).category)
        assertEquals(FileCategory.DOCUMENTS, FileItem(docFile).category)
    }

    @Test
    fun testFormattedSize() {
        val file = File("/dummy/test.bin")
        val item1 = FileItem(file = file, size = 1048576L, isDirectory = false)
        assertEquals("1.0 MB", item1.formattedSize())

        val item2 = FileItem(file = file, size = 1073741824L, isDirectory = false)
        assertEquals("1.0 GB", item2.formattedSize())
    }
}

package com.oscan.android.engine

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidPdfExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val pdfExporter = AndroidPdfExporter()

    @Test
    fun exportToPdfSinglePageProducesNonEmptyStream() {
        val bitmap = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)

        val outputStream = ByteArrayOutputStream()
        pdfExporter.exportToPdf(bitmap, outputStream)

        val bytes = outputStream.toByteArray()
        assertTrue(bytes.isNotEmpty())
        // PDF header check "%PDF"
        val header = String(bytes.take(4).toByteArray())
        assertEquals("%PDF", header)
    }

    @Test
    fun exportBitmapsToPdfMultiPageProducesNonEmptyStream() {
        val page1 = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val page2 = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val page3 = Bitmap.createBitmap(250, 250, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

        val outputStream = ByteArrayOutputStream()
        pdfExporter.exportBitmapsToPdf(listOf(page1, page2, page3), outputStream)

        val bytes = outputStream.toByteArray()
        assertTrue(bytes.size > 100)
        assertEquals("%PDF", String(bytes.take(4).toByteArray()))
    }

    @Test
    fun exportPageFilesToPdfMultiPageReadsFilesAndGeneratesPdf() {
        val file1 = tempFolder.newFile("page1.jpg")
        val file2 = tempFolder.newFile("page2.jpg")

        val b1 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val b2 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

        FileOutputStream(file1).use { b1.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        FileOutputStream(file2).use { b2.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        val pdfFile = tempFolder.newFile("output.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfExporter.exportPageFilesToPdf(listOf(file1, file2), out)
        }

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportBitmapsToPdfEmptyListThrows() {
        pdfExporter.exportBitmapsToPdf(emptyList(), ByteArrayOutputStream())
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportPageFilesToPdfEmptyListThrows() {
        pdfExporter.exportPageFilesToPdf(emptyList(), ByteArrayOutputStream())
    }
}

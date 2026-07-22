package com.oscan.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oscan.android.engine.AndroidPdfExporter
import com.oscan.android.engine.AndroidScannerEngine
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import com.oscan.core.util.CornerValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.core.Point
import kotlinx.coroutines.runBlocking
import java.io.File

@RunWith(AndroidJUnit4::class)
class PipelineIntegrationTest {

    @Test
    fun openCvNativeLibraryInitializes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("OpenCV Android native library should initialize", AndroidScannerEngine(context).initialize())
    }

    @Test
    fun testFullPipelineProducesNonEmptyPdf() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // 1. Synthesize a mock document image fixture
        val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(100f, 100f, 700f, 900f, paint)

        // 2. Validate corner geometry
        val dimensions = ImageDimensions(bitmap.width, bitmap.height)
        val corners = CornerPoints(
            topLeft = Point(100.0, 100.0),
            topRight = Point(700.0, 100.0),
            bottomRight = Point(700.0, 900.0),
            bottomLeft = Point(100.0, 900.0)
        )
        assertTrue(CornerValidator.isValidQuadrilateral(corners.toArray(), dimensions))

        // 3. Export to PDF
        val pdfExporter = AndroidPdfExporter()
        val outputFile = File(context.cacheDir, "integration_test_output.pdf")
        if (outputFile.exists()) outputFile.delete()

        pdfExporter.exportToPdf(bitmap, outputFile)

        // 4. Verify non-empty PDF created
        assertTrue("Output PDF should exist", outputFile.exists())
        assertTrue("Output PDF size should be > 0 bytes", outputFile.length() > 0)

        outputFile.delete()
        bitmap.recycle()
    }

    @Test
    fun testMultiPagePdfExportProducesNonEmptyPdf() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val bitmap1 = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888).apply { Canvas(this).drawColor(Color.RED) }
        val bitmap2 = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888).apply { Canvas(this).drawColor(Color.BLUE) }

        val file1 = File(context.cacheDir, "test_page1.jpg")
        val file2 = File(context.cacheDir, "test_page2.jpg")
        java.io.FileOutputStream(file1).use { bitmap1.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        java.io.FileOutputStream(file2).use { bitmap2.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        val pdfExporter = AndroidPdfExporter()
        val outputFile = File(context.cacheDir, "integration_test_multipage.pdf")
        if (outputFile.exists()) outputFile.delete()

        java.io.FileOutputStream(outputFile).use { out ->
            pdfExporter.exportPageFilesToPdf(listOf(file1, file2), out)
        }

        assertTrue("Multi-page output PDF should exist", outputFile.exists())
        assertTrue("Multi-page output PDF size should be > 0 bytes", outputFile.length() > 0)

        outputFile.delete()
        file1.delete()
        file2.delete()
        bitmap1.recycle()
        bitmap2.recycle()
    }
}

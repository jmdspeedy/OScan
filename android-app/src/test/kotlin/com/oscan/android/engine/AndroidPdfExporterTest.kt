package com.oscan.android.engine

import com.oscan.android.data.preferences.JpegQuality
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertTrue

/**
 * JVM-safe validation tests. Rendering is covered by PipelineIntegrationTest on Android because
 * Robolectric does not provide a functional implementation of the platform PdfDocument API.
 */
class AndroidPdfExporterTest {
    private val pdfExporter = AndroidPdfExporter()

    @Test(expected = IllegalArgumentException::class)
    fun exportBitmapsToPdfEmptyListThrows() {
        pdfExporter.exportBitmapsToPdf(emptyList(), ByteArrayOutputStream())
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportPageFilesToPdfEmptyListThrows() {
        pdfExporter.exportPageFilesToPdf(emptyList(), ByteArrayOutputStream())
    }

    @Test
    fun pdfQualityProfilesReduceResolutionAndCompressionMonotonically() {
        assertTrue(JpegQuality.HIGH.pdfMaxDimension > JpegQuality.MEDIUM.pdfMaxDimension)
        assertTrue(JpegQuality.MEDIUM.pdfMaxDimension > JpegQuality.LOW.pdfMaxDimension)
        assertTrue(JpegQuality.HIGH.pdfJpegQuality > JpegQuality.MEDIUM.pdfJpegQuality)
        assertTrue(JpegQuality.MEDIUM.pdfJpegQuality > JpegQuality.LOW.pdfJpegQuality)
    }
}

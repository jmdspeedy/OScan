package com.oscan.android.engine

import org.junit.Test
import java.io.ByteArrayOutputStream

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
}

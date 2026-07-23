package com.oscan.android.engine

import java.io.ByteArrayOutputStream
import org.junit.Test

class AndroidDocumentExporterTest {
    private val documentExporter = AndroidDocumentExporter()

    @Test(expected = IllegalArgumentException::class)
    fun exportEmptyPageListThrows() {
        documentExporter.exportDocument(
            pages = emptyList(),
            outputStream = ByteArrayOutputStream(),
            format = ExportFormat.PDF
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportEmptyPageListPngThrows() {
        documentExporter.exportDocument(
            pages = emptyList(),
            outputStream = ByteArrayOutputStream(),
            format = ExportFormat.PNG
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportEmptyPageListJpgThrows() {
        documentExporter.exportDocument(
            pages = emptyList(),
            outputStream = ByteArrayOutputStream(),
            format = ExportFormat.JPG
        )
    }
}

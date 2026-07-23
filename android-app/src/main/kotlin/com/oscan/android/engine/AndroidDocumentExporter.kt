package com.oscan.android.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.oscan.android.data.preferences.JpegQuality
import com.oscan.android.data.preferences.PdfPageSize
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    PDF("PDF", "pdf", "application/pdf"),
    PNG("PNG", "png", "image/png"),
    JPG("JPG", "jpg", "image/jpeg")
}

class AndroidDocumentExporter(
    private val pdfExporter: AndroidPdfExporter = AndroidPdfExporter()
) {

    fun exportDocument(
        pages: List<PdfPageSpec>,
        outputStream: OutputStream,
        format: ExportFormat,
        pageSize: PdfPageSize = PdfPageSize.A4,
        quality: JpegQuality = JpegQuality.HIGH,
        documentName: String = "document"
    ) {
        require(pages.isNotEmpty()) { "Cannot export empty page list" }
        when (format) {
            ExportFormat.PDF -> {
                pdfExporter.exportPageSpecsToPdf(pages, outputStream, pageSize, quality)
            }
            ExportFormat.PNG, ExportFormat.JPG -> {
                val compressFormat = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val compressQuality = if (format == ExportFormat.PNG) 100 else quality.qualityInt

                if (pages.size == 1) {
                    writeSinglePageImage(pages[0], outputStream, compressFormat, compressQuality)
                } else {
                    writeMultiPageZip(pages, outputStream, compressFormat, compressQuality, format.extension, documentName)
                }
            }
        }
    }

    private fun writeSinglePageImage(
        spec: PdfPageSpec,
        outputStream: OutputStream,
        compressFormat: Bitmap.CompressFormat,
        quality: Int
    ) {
        val bitmap = loadAndRotateBitmap(spec)
        try {
            bitmap.compress(compressFormat, quality, outputStream)
        } finally {
            bitmap.recycle()
        }
    }

    private fun writeMultiPageZip(
        pages: List<PdfPageSpec>,
        outputStream: OutputStream,
        compressFormat: Bitmap.CompressFormat,
        quality: Int,
        extension: String,
        documentName: String
    ) {
        ZipOutputStream(outputStream).use { zip ->
            pages.forEachIndexed { index, spec ->
                val safeDocName = documentName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val entryName = "${safeDocName}_page_${index + 1}.$extension"
                zip.putNextEntry(ZipEntry(entryName))
                val bitmap = loadAndRotateBitmap(spec)
                try {
                    bitmap.compress(compressFormat, quality, zip)
                } finally {
                    bitmap.recycle()
                }
                zip.closeEntry()
            }
        }
    }

    fun loadAndRotateBitmap(spec: PdfPageSpec): Bitmap {
        val file = spec.file
        require(file.exists() && file.isFile) { "Page image file does not exist: ${file.path}" }
        val bitmap = BitmapFactory.decodeFile(file.path)
            ?: throw IllegalStateException("Could not decode image file: ${file.path}")
        val rotation = (spec.rotationDegrees % 360 + 360) % 360
        if (rotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
}

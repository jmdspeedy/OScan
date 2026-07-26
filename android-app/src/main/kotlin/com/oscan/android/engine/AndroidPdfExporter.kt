package com.oscan.android.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

data class PdfPageSpec(
    val file: File,
    val rotationDegrees: Int = 0
)

/**
 * Exports single-page or multi-page PDF documents using Android's platform [PdfDocument] API.
 */
class AndroidPdfExporter {

    companion object {
        // Standard A4 dimensions in points (72 points per inch)
        const val A4_WIDTH_PTS = 595
        const val A4_HEIGHT_PTS = 842
    }

    /**
     * Renders [bitmap] centered on an A4 PDF page and writes to [outputStream].
     */
    fun exportToPdf(bitmap: Bitmap, outputStream: OutputStream) {
        exportBitmapsToPdf(listOf(bitmap), outputStream)
    }

    /**
     * Renders [bitmap] centered on an A4 PDF page and writes to target [outputFile].
     */
    fun exportToPdf(bitmap: Bitmap, outputFile: File) {
        FileOutputStream(outputFile).use { stream ->
            exportToPdf(bitmap, stream)
        }
    }

    /**
     * Renders multiple page bitmaps in [bitmaps] order centered on A4 PDF pages and writes to [outputStream].
     */
    fun exportBitmapsToPdf(bitmaps: List<Bitmap>, outputStream: OutputStream) {
        if (bitmaps.isEmpty()) throw IllegalArgumentException("Cannot export empty page list to PDF")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            bitmaps.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, index + 1).create()
                val page = document.startPage(pageInfo)
                try {
                    val canvas: Canvas = page.canvas
                    val imgWidth = bitmap.width.toFloat()
                    val imgHeight = bitmap.height.toFloat()

                    val scale = min(A4_WIDTH_PTS / imgWidth, A4_HEIGHT_PTS / imgHeight)
                    val scaledWidth = imgWidth * scale
                    val scaledHeight = imgHeight * scale

                    val dx = (A4_WIDTH_PTS - scaledWidth) / 2.0f
                    val dy = (A4_HEIGHT_PTS - scaledHeight) / 2.0f

                    val destRect = RectF(dx, dy, dx + scaledWidth, dy + scaledHeight)
                    canvas.drawBitmap(bitmap, null, destRect, paint)
                } finally {
                    document.finishPage(page)
                }
            }
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    /**
     * Reads image files from [pageFiles] in order, renders each page onto an A4 PDF page,
     * and writes the document to [outputStream].
     */
    fun exportPageFilesToPdf(pageFiles: List<File>, outputStream: OutputStream) {
        exportPageSpecsToPdf(pageFiles.map { PdfPageSpec(it, 0) }, outputStream)
    }

    /**
     * Reads image files from [pages] in order, applies rotation, renders onto PDF pages
     * configured by [pageSize], and writes the document to [outputStream].
     */
    fun exportPageSpecsToPdf(
        pages: List<PdfPageSpec>,
        outputStream: OutputStream,
        pageSize: com.oscan.android.data.preferences.PdfPageSize = com.oscan.android.data.preferences.PdfPageSize.A4,
        quality: com.oscan.android.data.preferences.JpegQuality = com.oscan.android.data.preferences.JpegQuality.HIGH
    ) {
        if (pages.isEmpty()) throw IllegalArgumentException("Cannot export empty page list to PDF")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            pages.forEachIndexed { index, spec ->
                val file = spec.file
                val rotation = (spec.rotationDegrees % 360 + 360) % 360
                require(file.exists() && file.isFile) { "Page image file does not exist: ${file.path}" }
                val bitmap = decodePdfBitmap(file, quality)
                    ?: throw IllegalStateException("Could not decode image file: ${file.path}")
                try {
                    val isRotated90 = (rotation % 180 != 0)
                    val effectiveWidth = if (isRotated90) bitmap.height.toFloat() else bitmap.width.toFloat()
                    val effectiveHeight = if (isRotated90) bitmap.width.toFloat() else bitmap.height.toFloat()

                    val targetWidthPts: Float
                    val targetHeightPts: Float
                    when (pageSize) {
                        com.oscan.android.data.preferences.PdfPageSize.A4 -> {
                            targetWidthPts = A4_WIDTH_PTS.toFloat()
                            targetHeightPts = A4_HEIGHT_PTS.toFloat()
                        }
                        com.oscan.android.data.preferences.PdfPageSize.LETTER -> {
                            targetWidthPts = 612f
                            targetHeightPts = 792f
                        }
                        com.oscan.android.data.preferences.PdfPageSize.MATCH_PAGE -> {
                            targetWidthPts = effectiveWidth
                            targetHeightPts = effectiveHeight
                        }
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(targetWidthPts.toInt(), targetHeightPts.toInt(), index + 1).create()
                    val page = document.startPage(pageInfo)
                    try {
                        val canvas: Canvas = page.canvas
                        val scale = min(targetWidthPts / effectiveWidth, targetHeightPts / effectiveHeight)
                        val scaledWidth = effectiveWidth * scale
                        val scaledHeight = effectiveHeight * scale

                        val dx = (targetWidthPts - scaledWidth) / 2.0f
                        val dy = (targetHeightPts - scaledHeight) / 2.0f

                        if (rotation == 0) {
                            val destRect = RectF(dx, dy, dx + scaledWidth, dy + scaledHeight)
                            canvas.drawBitmap(bitmap, null, destRect, paint)
                        } else {
                            canvas.save()
                            canvas.translate(dx + scaledWidth / 2f, dy + scaledHeight / 2f)
                            canvas.rotate(rotation.toFloat())
                            val unrotatedW = if (isRotated90) scaledHeight else scaledWidth
                            val unrotatedH = if (isRotated90) scaledWidth else scaledHeight
                            val destRect = RectF(-unrotatedW / 2f, -unrotatedH / 2f, unrotatedW / 2f, unrotatedH / 2f)
                            canvas.drawBitmap(bitmap, null, destRect, paint)
                            canvas.restore()
                        }
                    } finally {
                        document.finishPage(page)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun decodePdfBitmap(
        file: File,
        quality: com.oscan.android.data.preferences.JpegQuality
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > quality.pdfMaxDimension * 2) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: return null

        val longestEdge = max(decoded.width, decoded.height)
        val scaled = if (longestEdge > quality.pdfMaxDimension) {
            val scale = quality.pdfMaxDimension.toFloat() / longestEdge.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        val compressed = ByteArrayOutputStream().use { buffer ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, quality.pdfJpegQuality, buffer)) {
                "Could not compress PDF page image: ${file.path}"
            }
            buffer.toByteArray()
        }
        scaled.recycle()
        return BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
    }
}

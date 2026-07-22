package com.oscan.android.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.min

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
     * and writes the document to [outputStream]. Bitmaps are decoded sequentially to minimize memory usage.
     */
    fun exportPageFilesToPdf(pageFiles: List<File>, outputStream: OutputStream) {
        if (pageFiles.isEmpty()) throw IllegalArgumentException("Cannot export empty page list to PDF")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            pageFiles.forEachIndexed { index, file ->
                require(file.exists() && file.isFile) { "Page image file does not exist: ${file.path}" }
                val bitmap = BitmapFactory.decodeFile(file.path)
                    ?: throw IllegalStateException("Could not decode image file: ${file.path}")
                try {
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
                } finally {
                    bitmap.recycle()
                }
            }
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }
}


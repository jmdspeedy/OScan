package com.oscan.android.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.min

/**
 * Exports a single-page PDF document using Android's platform [PdfDocument] API.
 */
class AndroidPdfExporter {

    companion object {
        // Standard A4 dimensions in points (72 points per inch)
        private const val A4_WIDTH_PTS = 595
        private const val A4_HEIGHT_PTS = 842
    }

    /**
     * Renders [bitmap] centered on an A4 PDF page and writes to [outputStream].
     */
    fun exportToPdf(bitmap: Bitmap, outputStream: OutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, 1).create()
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
            canvas.drawBitmap(bitmap, null, destRect, null)
        } finally {
            document.finishPage(page)
        }

        try {
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    /**
     * Renders [bitmap] centered on an A4 PDF page and writes to target [outputFile].
     */
    fun exportToPdf(bitmap: Bitmap, outputFile: File) {
        FileOutputStream(outputFile).use { stream ->
            exportToPdf(bitmap, stream)
        }
    }
}

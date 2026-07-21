package com.oscan.core

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File

class PdfExporter {
    fun exportToPdf(imagePath: String, outputPath: String) {
        val document = PDDocument()
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)
        
        val pdImage = PDImageXObject.createFromFile(imagePath, document)
        
        val contentStream = PDPageContentStream(document, page)
        
        // Scale image to fit A4 page
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height
        
        val imageWidth = pdImage.width.toFloat()
        val imageHeight = pdImage.height.toFloat()
        
        val scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        
        val x = (pageWidth - scaledWidth) / 2
        val y = (pageHeight - scaledHeight) / 2
        
        contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight)
        contentStream.close()
        
        document.save(File(outputPath))
        document.close()
    }
}

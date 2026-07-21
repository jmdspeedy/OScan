package com.oscan.desktop

import com.oscan.core.DocumentScanner
import com.oscan.core.ImageEnhancer
import com.oscan.core.PdfExporter
import nu.pattern.OpenCV
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

fun main() {
    // Load OpenCV native library dynamically across OS
    OpenCV.loadLocally()
    
    val inputDir = File("test-images")
    if (!inputDir.exists() || !inputDir.isDirectory) {
        println("Error: Please provide a test-images directory.")
        return
    }
    
    val outputDir = File(inputDir, "output")
    if (!outputDir.exists()) outputDir.mkdirs()
    
    val images = inputDir.listFiles { _, name ->
        name.endsWith(".jpg", ignoreCase = true) && !name.contains("_expected", ignoreCase = true)
    }?.sortedBy { it.name }
    if (images.isNullOrEmpty()) {
        println("No jpg images found in test-images.")
        return
    }
    
    val scanner = DocumentScanner()
    val enhancer = ImageEnhancer()
    val pdfExporter = PdfExporter()
    
    for (inputFile in images) {
        val fileName = inputFile.nameWithoutExtension
        println("\nProcessing ${inputFile.name}...")
        
        val source = Imgcodecs.imread(inputFile.absolutePath)
        if (source.empty()) {
            println("Error: Failed to load ${inputFile.name}.")
            continue
        }
        
        // 1. Box Label
        val corners = scanner.detectCorners(source)
        if (corners == null) {
            println("Error: Could not detect a document in ${inputFile.name}.")
            continue
        }
        
        val boxImg = source.clone()
        val green = org.opencv.core.Scalar(170.0, 255.0, 0.0) // BGR for cyan/green
        
        // Draw the edges
        val ptsList = listOf(org.opencv.core.MatOfPoint(*corners))
        org.opencv.imgproc.Imgproc.polylines(boxImg, ptsList, true, green, 10)
        
        // Draw circles at corners
        for (corner in corners) {
            org.opencv.imgproc.Imgproc.circle(boxImg, corner, 30, org.opencv.core.Scalar(255.0, 255.0, 255.0), -1)
            org.opencv.imgproc.Imgproc.circle(boxImg, corner, 30, green, 10)
        }
        
        // Draw rounded rectangles (pill shape) at midpoints
        for (i in 0 until 4) {
            val pt1 = corners[i]
            val pt2 = corners[(i + 1) % 4]
            val midX = (pt1.x + pt2.x) / 2.0
            val midY = (pt1.y + pt2.y) / 2.0
            
            // Calculate angle
            val angle = Math.atan2(pt2.y - pt1.y, pt2.x - pt1.x) * 180.0 / Math.PI
            
            val rect = org.opencv.core.RotatedRect(
                org.opencv.core.Point(midX, midY),
                org.opencv.core.Size(120.0, 40.0),
                angle
            )
            val box = arrayOfNulls<org.opencv.core.Point>(4)
            rect.points(box)
            
            // Draw filled white polygon
            val pillPoints = listOf(org.opencv.core.MatOfPoint(*box.requireNoNulls()))
            org.opencv.imgproc.Imgproc.fillPoly(boxImg, pillPoints, org.opencv.core.Scalar(255.0, 255.0, 255.0))
            
            // Draw green outline
            org.opencv.imgproc.Imgproc.polylines(boxImg, pillPoints, true, green, 10)
        }
        
        val boxPath = File(outputDir, "${fileName}_step1_box.jpg").absolutePath
        Imgcodecs.imwrite(boxPath, boxImg)
        println("Saved box label image to $boxPath")
        
        // 2. Edge Crop
        val cropped = scanner.cropWarped(source, corners)
        
        val croppedPath = File(outputDir, "${fileName}_step2_cropped.jpg").absolutePath
        Imgcodecs.imwrite(croppedPath, cropped)
        println("Saved cropped image to $croppedPath")
        
        // 3. Magic Filter
        val filtered = enhancer.applyMagicFilter(cropped)
        
        val filteredPath = File(outputDir, "${fileName}_step3_magic.jpg").absolutePath
        Imgcodecs.imwrite(filteredPath, filtered)
        println("Saved enhanced image to $filteredPath")
        
        // 4. Export PDF
        val pdfPath = File(outputDir, "${fileName}_step4_output.pdf").absolutePath
        pdfExporter.exportToPdf(filteredPath, pdfPath)
        println("Saved PDF to $pdfPath")
    }
    
    println("\nPipeline completed successfully for all images!")
}

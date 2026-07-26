package com.oscan.desktop

import com.oscan.core.IdCardProcessor
import com.oscan.core.PdfExporter
import java.io.File
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

private data class CardPair(val name: String, val front: File, val back: File)

fun main() {
    OpenCV.loadLocally()
    val inputDir = File("test-images")
    val outputDir = File(inputDir, "output/id-cards").apply { mkdirs() }
    val pairs = findCardPairs(inputDir)
    check(pairs.isNotEmpty()) { "No card_test*_front.jpg/card_test*_back.jpg pairs found" }

    val processor = IdCardProcessor()
    val pdfExporter = PdfExporter()
    val failures = mutableListOf<String>()
    pairs.forEach { pair ->
        println("\nProcessing ${pair.name}...")
        val front = processSide(pair.front, outputDir, processor)
        val back = processSide(pair.back, outputDir, processor)
        if (front == null || back == null) {
            failures += pair.name
            front?.release()
            back?.release()
            return@forEach
        }

        val sheet = processor.createSheet(front, back)
        val sheetFile = File(outputDir, "${pair.name}_step4_sheet.jpg")
        Imgcodecs.imwrite(sheetFile.absolutePath, sheet)
        val pdfFile = File(outputDir, "${pair.name}_step5_output.pdf")
        pdfExporter.exportToPdf(sheetFile.absolutePath, pdfFile.absolutePath)
        println("Saved combined sheet to ${sheetFile.absolutePath}")
        println("Saved PDF to ${pdfFile.absolutePath}")

        sheet.release()
        front.release()
        back.release()
    }

    check(failures.isEmpty()) { "ID-card pipeline failed for: ${failures.joinToString()}" }
    println("\nID-card pipeline completed successfully for ${pairs.size} front/back pairs.")
}

private fun processSide(input: File, outputDir: File, processor: IdCardProcessor): Mat? {
    val source = Imgcodecs.imread(input.absolutePath)
    if (source.empty()) {
        println("Error: Failed to load ${input.name}")
        return null
    }
    val corners = processor.detectCorners(source)
    if (corners == null) {
        println("Error: Could not detect four card edges in ${input.name}")
        source.release()
        return null
    }
    println("${input.name}: ${corners.joinToString { "(%.1f, %.1f)".format(it.x, it.y) }}")

    val baseName = input.nameWithoutExtension
    val overlay = source.clone()
    Imgproc.polylines(overlay, listOf(MatOfPoint(*corners)), true, Scalar(170.0, 255.0, 0.0), 10)
    corners.forEach { corner -> Imgproc.circle(overlay, corner, 24, Scalar(170.0, 255.0, 0.0), 8) }
    Imgcodecs.imwrite(File(outputDir, "${baseName}_step1_edges.jpg").absolutePath, overlay)

    val rectangular = processor.cropRectangle(source, corners)
    Imgcodecs.imwrite(File(outputDir, "${baseName}_step2_rectangle.jpg").absolutePath, rectangular)
    val rounded = processor.applyRoundedCorners(rectangular)
    Imgcodecs.imwrite(File(outputDir, "${baseName}_step3_rounded.jpg").absolutePath, rounded)

    overlay.release()
    rounded.release()
    source.release()
    return rectangular
}

private fun findCardPairs(inputDir: File): List<CardPair> {
    val pattern = Regex("^(card_test.+)_(front|back)\\.jpg$", RegexOption.IGNORE_CASE)
    val grouped = inputDir.listFiles()
        .orEmpty()
        .mapNotNull { file -> pattern.matchEntire(file.name)?.let { it.groupValues[1] to (it.groupValues[2].lowercase() to file) } }
        .groupBy({ it.first }, { it.second })
    return grouped.entries.sortedBy { it.key }.mapNotNull { (name, sides) ->
        val files = sides.toMap()
        val front = files["front"]
        val back = files["back"]
        if (front == null || back == null) {
            println("Skipping $name: both front and back fixtures are required")
            null
        } else {
            CardPair(name, front, back)
        }
    }
}

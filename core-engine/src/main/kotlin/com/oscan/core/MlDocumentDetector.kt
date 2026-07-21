package com.oscan.core

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Offline document-corner inference using MakeACopy's Apache-2.0 DocQuadNet-256 model.
 *
 * The model consumes a mid-gray-letterboxed 256x256 RGB tensor and returns one 64x64 heatmap for
 * each corner in TL/TR/BR/BL order. No image or telemetry leaves the device.
 */
internal class MlDocumentDetector private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession
) : AutoCloseable {
    private data class Letterbox(
        val scale: Double,
        val offsetX: Double,
        val offsetY: Double
    )

    fun detect(source: Mat): Array<Point>? {
        if (source.empty()) return null
        val (input, letterbox) = preprocess(source)
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())

        val heatmaps = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf("input" to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                (result.get("corner_heatmaps").orElse(null)?.value as? Array<Array<Array<FloatArray>>>)
            }
        } ?: return null

        if (heatmaps.size != 1 || heatmaps[0].size != 4) return null
        val corners = Array(4) { channel ->
            val point256 = refinedPeak(heatmaps[0][channel])
            Point(
                (point256.x - letterbox.offsetX) / letterbox.scale,
                (point256.y - letterbox.offsetY) / letterbox.scale
            )
        }

        return if (isPlausible(corners, source.width(), source.height())) corners else null
    }

    private fun preprocess(source: Mat): Pair<FloatArray, Letterbox> {
        val scale = min(INPUT_SIZE.toDouble() / source.width(), INPUT_SIZE.toDouble() / source.height())
        val scaledWidth = max(1, (source.width() * scale).roundToInt())
        val scaledHeight = max(1, (source.height() * scale).roundToInt())
        val offsetX = (INPUT_SIZE - source.width() * scale) / 2.0
        val offsetY = (INPUT_SIZE - source.height() * scale) / 2.0
        val roiX = ((INPUT_SIZE - scaledWidth) / 2.0).roundToInt()
        val roiY = ((INPUT_SIZE - scaledHeight) / 2.0).roundToInt()

        val letterboxed = Mat(INPUT_SIZE, INPUT_SIZE, CvType.CV_8UC3, Scalar(128.0, 128.0, 128.0))
        val resized = Mat()
        Imgproc.resize(source, resized, Size(scaledWidth.toDouble(), scaledHeight.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)
        resized.copyTo(letterboxed.submat(roiY, roiY + scaledHeight, roiX, roiX + scaledWidth))

        val planeSize = INPUT_SIZE * INPUT_SIZE
        val tensor = FloatArray(planeSize * 3)
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val bgr = letterboxed.get(y, x)
                val index = y * INPUT_SIZE + x
                tensor[index] = (bgr[2] / 255.0).toFloat()
                tensor[planeSize + index] = (bgr[1] / 255.0).toFloat()
                tensor[planeSize * 2 + index] = (bgr[0] / 255.0).toFloat()
            }
        }
        return tensor to Letterbox(scale, offsetX, offsetY)
    }

    /** Parabolic sub-pixel peak refinement used by the upstream model integration. */
    private fun refinedPeak(heatmap: Array<FloatArray>): Point {
        var bestValue = -Float.MAX_VALUE
        var bestX = 0
        var bestY = 0
        for (y in heatmap.indices) {
            for (x in heatmap[y].indices) {
                if (heatmap[y][x] > bestValue) {
                    bestValue = heatmap[y][x]
                    bestX = x
                    bestY = y
                }
            }
        }

        var dx = 0.0
        if (bestX > 0 && bestX < OUTPUT_SIZE - 1) {
            val left = heatmap[bestY][bestX - 1].toDouble()
            val centre = heatmap[bestY][bestX].toDouble()
            val right = heatmap[bestY][bestX + 1].toDouble()
            val denominator = left - 2.0 * centre + right
            if (denominator < -1e-12) dx = (0.5 * (left - right) / denominator).coerceIn(-0.5, 0.5)
        }

        var dy = 0.0
        if (bestY > 0 && bestY < OUTPUT_SIZE - 1) {
            val top = heatmap[bestY - 1][bestX].toDouble()
            val centre = heatmap[bestY][bestX].toDouble()
            val bottom = heatmap[bestY + 1][bestX].toDouble()
            val denominator = top - 2.0 * centre + bottom
            if (denominator < -1e-12) dy = (0.5 * (top - bottom) / denominator).coerceIn(-0.5, 0.5)
        }
        return Point((bestX + 0.5 + dx) * 4.0, (bestY + 0.5 + dy) * 4.0)
    }

    private fun isPlausible(points: Array<Point>, width: Int, height: Int): Boolean {
        if (points.any { !it.x.isFinite() || !it.y.isFinite() }) return false
        if (points.any { it.x < -width * 0.12 || it.x > width * 1.12 || it.y < -height * 0.12 || it.y > height * 1.12 }) return false

        var sign = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % 4]
            val c = points[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (abs(cross) < 1e-6) return false
            if (sign == 0.0) sign = kotlin.math.sign(cross) else if (kotlin.math.sign(cross) != sign) return false
        }

        val area = abs(points.indices.sumOf { i ->
            val next = points[(i + 1) % 4]
            points[i].x * next.y - next.x * points[i].y
        }) / 2.0
        if (area < width * height * 0.12) return false

        val shortestEdge = points.indices.minOf { i ->
            val next = points[(i + 1) % 4]
            hypot(next.x - points[i].x, next.y - points[i].y)
        }
        return shortestEdge >= min(width, height) * 0.15
    }

    override fun close() {
        session.close()
    }

    companion object {
        private const val INPUT_SIZE = 256
        private const val OUTPUT_SIZE = 64
        private const val MODEL_RESOURCE = "/docquad/docquadnet256_trained_opset17.ort"

        fun loadDefaultOrNull(): MlDocumentDetector? {
            return try {
                val bytes = MlDocumentDetector::class.java.getResourceAsStream(MODEL_RESOURCE)?.use { it.readBytes() }
                    ?: return null
                val environment = OrtEnvironment.getEnvironment()
                val options = OrtSession.SessionOptions().apply {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    setIntraOpNumThreads(max(1, Runtime.getRuntime().availableProcessors() / 2))
                }
                MlDocumentDetector(environment, environment.createSession(bytes, options))
            } catch (_: Throwable) {
                null
            }
        }
    }
}

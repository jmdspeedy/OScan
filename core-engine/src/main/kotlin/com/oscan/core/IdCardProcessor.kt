package com.oscan.core

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

/** Shared ID-card crop and sheet generation used by Android and the desktop fixture runner. */
class IdCardProcessor(
    private val scanner: DocumentScanner = DocumentScanner()
) {
    fun detectCorners(source: Mat): Array<Point>? {
        if (source.empty() || source.cols() < 32 || source.rows() < 32) return null
        return detectCardContour(source)
            ?: scanner.detectCorners(source)?.takeIf(::hasPlausibleCardGeometry)
    }

    /** Recovers the four straight card edges at the physical ISO/IEC 7810 ID-1 aspect ratio. */
    fun cropRectangle(source: Mat, corners: Array<Point>): Mat =
        scanner.cropWarped(source, corners, ID_CARD_ASPECT_RATIO)

    /** Removes the small background wedges outside the physical card's rounded corners. */
    fun applyRoundedCorners(card: Mat): Mat {
        require(!card.empty()) { "Card image must not be empty" }
        val output = Mat(card.size(), card.type(), whiteFor(card.channels()))
        val mask = roundedMask(card.cols(), card.rows())
        card.copyTo(output, mask)
        mask.release()
        return output
    }

    /** Places the front and back on one white A-series-ratio page, matching Android export. */
    fun createSheet(front: Mat, back: Mat): Mat {
        require(!front.empty() && !back.empty()) { "Both card sides are required" }
        val sheet = Mat(SHEET_HEIGHT, SHEET_WIDTH, CvType.CV_8UC3, Scalar.all(255.0))
        val frontSize = fittedSize(front)
        val backSize = fittedSize(back)
        val totalHeight = frontSize.height + SHEET_GAP + backSize.height
        val startY = ((SHEET_HEIGHT - totalHeight) / 2.0).roundToInt()

        drawCard(sheet, front, frontSize, startY)
        drawCard(sheet, back, backSize, startY + frontSize.height.roundToInt() + SHEET_GAP)
        return sheet
    }

    private fun drawCard(sheet: Mat, card: Mat, size: Size, top: Int) {
        val width = size.width.roundToInt().coerceAtLeast(2)
        val height = size.height.roundToInt().coerceAtLeast(2)
        val left = (SHEET_WIDTH - width) / 2
        val resized = Mat()
        Imgproc.resize(card, resized, Size(width.toDouble(), height.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
        val rounded = applyRoundedCorners(resized)
        val destination = sheet.submat(Rect(left, top, width, height))
        rounded.copyTo(destination)
        destination.release()
        rounded.release()
        resized.release()
    }

    private fun fittedSize(card: Mat): Size {
        val scale = min(MAX_CARD_WIDTH / card.cols(), MAX_CARD_HEIGHT / card.rows())
        return Size(card.cols() * scale, card.rows() * scale)
    }

    /**
     * Rounded cards often do not produce four literal contour corners. Build a convex hull from
     * threshold/edge contours, then approximate its four straight sides and rank by the known
     * ID-1 aspect ratio. This prevents a large table, mat, or preview boundary from winning merely
     * because it encloses more of the frame.
     */
    private fun detectCardContour(source: Mat): Array<Point>? {
        val scale = min(1.0, DETECTION_MAX_EDGE / max(source.cols(), source.rows()).toDouble())
        val working = Mat()
        if (scale < 1.0) {
            Imgproc.resize(source, working, Size(), scale, scale, Imgproc.INTER_AREA)
        } else {
            source.copyTo(working)
        }
        val gray = Mat()
        Imgproc.cvtColor(working, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val threshold = Mat()
        Imgproc.threshold(gray, threshold, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
        val closeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(17.0, 17.0))
        Imgproc.morphologyEx(threshold, threshold, Imgproc.MORPH_CLOSE, closeKernel)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 30.0, 100.0, 3, true)
        val edgeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(9.0, 9.0))
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, edgeKernel)

        var best: Array<Point>? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (mask in listOf(threshold, edges)) {
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            val contourInput = mask.clone()
            Imgproc.findContours(contourInput, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            contourInput.release()
            hierarchy.release()
            for (contour in contours) {
                val contourArea = abs(Imgproc.contourArea(contour))
                val areaFraction = contourArea / (working.cols().toDouble() * working.rows())
                if (areaFraction !in 0.045..0.72) {
                    contour.release()
                    continue
                }
                val hullIndices = MatOfInt()
                Imgproc.convexHull(contour, hullIndices)
                val contourPoints = contour.toArray()
                val hull = MatOfPoint2f(*hullIndices.toArray().map { contourPoints[it] }.toTypedArray())
                hullIndices.release()
                val perimeter = Imgproc.arcLength(hull, true)
                for (epsilonFraction in CARD_APPROXIMATION_EPSILONS) {
                    val approximation = MatOfPoint2f()
                    Imgproc.approxPolyDP(hull, approximation, perimeter * epsilonFraction, true)
                    if (approximation.total() == 4L) {
                        val candidate = orderPoints(approximation.toArray())
                        val score = cardCandidateScore(candidate, contourArea, working.cols(), working.rows())
                        if (score > bestScore) {
                            bestScore = score
                            best = candidate
                        }
                    }
                    approximation.release()
                }
                hull.release()
                contour.release()
            }
        }

        gray.release()
        threshold.release()
        edges.release()
        closeKernel.release()
        edgeKernel.release()
        working.release()
        if (best == null || bestScore < MIN_CARD_SCORE) return null
        val inverseScale = 1.0 / scale
        return best.map { Point(it.x * inverseScale, it.y * inverseScale) }.toTypedArray()
    }

    private fun cardCandidateScore(
        points: Array<Point>,
        contourArea: Double,
        imageWidth: Int,
        imageHeight: Int
    ): Double {
        if (!hasPlausibleCardGeometry(points)) return Double.NEGATIVE_INFINITY
        val quadArea = abs(Imgproc.contourArea(MatOfPoint2f(*points))).coerceAtLeast(1.0)
        val areaFraction = quadArea / (imageWidth.toDouble() * imageHeight)
        if (areaFraction !in 0.045..0.72) return Double.NEGATIVE_INFINITY
        val ratio = cardAspect(points)
        val aspectScore = (1.0 - abs(ln(ratio / ID_CARD_ASPECT_RATIO)) / ln(1.45)).coerceIn(0.0, 1.0)
        val fillScore = (contourArea / quadArea).coerceIn(0.0, 1.0)
        val centreX = points.map(Point::x).average()
        val centreY = points.map(Point::y).average()
        val centreDistance = hypot(centreX - imageWidth / 2.0, centreY - imageHeight / 2.0) /
            hypot(imageWidth.toDouble(), imageHeight.toDouble())
        val centreScore = (1.0 - centreDistance * 2.0).coerceIn(0.0, 1.0)
        val areaScore = (areaFraction / 0.22).coerceIn(0.0, 1.0)
        return aspectScore * 4.0 + fillScore * 2.0 + centreScore + areaScore
    }

    private fun hasPlausibleCardGeometry(points: Array<Point>): Boolean =
        points.size == 4 && cardAspect(points) in 1.28..1.95

    private fun cardAspect(points: Array<Point>): Double {
        if (points.size != 4) return 0.0
        val top = distance(points[0], points[1])
        val right = distance(points[1], points[2])
        val bottom = distance(points[2], points[3])
        val left = distance(points[3], points[0])
        val first = (top + bottom) / 2.0
        val second = (left + right) / 2.0
        return max(first, second) / min(first, second).coerceAtLeast(1.0)
    }

    private fun distance(first: Point, second: Point): Double = hypot(second.x - first.x, second.y - first.y)

    private fun orderPoints(points: Array<Point>): Array<Point> {
        val centreX = points.map(Point::x).average()
        val centreY = points.map(Point::y).average()
        val clockwise = points.sortedBy { kotlin.math.atan2(it.y - centreY, it.x - centreX) }
        val topLeft = clockwise.indices.minByOrNull { clockwise[it].x + clockwise[it].y } ?: 0
        return Array(4) { clockwise[(topLeft + it) % 4] }
    }

    private fun roundedMask(width: Int, height: Int): Mat {
        val mask = Mat.zeros(height, width, CvType.CV_8UC1)
        val radius = (width * CORNER_RADIUS_FRACTION).roundToInt()
            .coerceIn(1, min(width, height) / 2)
        val white = Scalar.all(255.0)
        Imgproc.rectangle(mask, Point(radius.toDouble(), 0.0), Point((width - radius).toDouble(), height.toDouble()), white, -1)
        Imgproc.rectangle(mask, Point(0.0, radius.toDouble()), Point(width.toDouble(), (height - radius).toDouble()), white, -1)
        listOf(
            Point(radius.toDouble(), radius.toDouble()),
            Point((width - radius - 1).toDouble(), radius.toDouble()),
            Point((width - radius - 1).toDouble(), (height - radius - 1).toDouble()),
            Point(radius.toDouble(), (height - radius - 1).toDouble())
        ).forEach { Imgproc.circle(mask, it, radius, white, -1, Imgproc.LINE_AA) }
        return mask
    }

    private fun whiteFor(channels: Int): Scalar = when (channels) {
        1 -> Scalar.all(255.0)
        3 -> Scalar(255.0, 255.0, 255.0)
        4 -> Scalar(255.0, 255.0, 255.0, 255.0)
        else -> error("Unsupported card channel count: $channels")
    }

    companion object {
        const val ID_CARD_ASPECT_RATIO = 1.586
        const val SHEET_WIDTH = 1240
        const val SHEET_HEIGHT = 1754
        private const val MAX_CARD_WIDTH = 960.0
        private const val MAX_CARD_HEIGHT = 600.0
        private const val SHEET_GAP = 110
        private const val CORNER_RADIUS_FRACTION = 0.037
        private const val DETECTION_MAX_EDGE = 1200.0
        private const val MIN_CARD_SCORE = 5.25
        private val CARD_APPROXIMATION_EPSILONS = doubleArrayOf(0.008, 0.012, 0.018, 0.025, 0.035, 0.05)
    }
}

package com.oscan.core

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Detects a document as four independently observed straight borders.
 *
 * This deliberately does not require the borders to form a connected contour. A Hough/LSD
 * line detector can still recover a border when glare, shadows, or a similarly coloured
 * background leave gaps in the edge. Candidate line pairs are intersected and the resulting
 * quadrilaterals are ranked using geometry, edge support, and colour change across each side.
 */
class DocumentScanner {
    private val mlDetector: MlDocumentDetector? by lazy { MlDocumentDetector.loadDefaultOrNull() }
    private data class LineCandidate(
        val p1: Point,
        val p2: Point,
        val angle: Double,
        val a: Double,
        val b: Double,
        val c: Double,
        val observedLength: Double
    ) {
        fun distance(point: Point): Double = abs(a * point.x + b * point.y + c)
        fun midpoint(): Point = Point((p1.x + p2.x) / 2.0, (p1.y + p2.y) / 2.0)
    }

    private data class OppositePair(
        val first: LineCandidate,
        val second: LineCandidate,
        val angle: Double,
        val separation: Double
    )

    fun detectCorners(source: Mat): Array<Point>? {
        if (source.empty() || source.width() < 32 || source.height() < 32) return null

        // Learned corner heatmaps handle white-on-white and heavily textured scenes. The line
        // detector below remains a fully classical fallback if the model cannot load or rejects
        // an implausible result.
        mlDetector?.detect(source)?.let { return it }

        val maxDimension = 900.0
        val scale = min(1.0, maxDimension / max(source.width(), source.height()).toDouble())
        val working = Mat()
        if (scale < 1.0) {
            Imgproc.resize(source, working, Size(), scale, scale, Imgproc.INTER_AREA)
        } else {
            source.copyTo(working)
        }

        val gray = Mat()
        Imgproc.cvtColor(working, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // CLAHE exposes weak paper/background transitions without requiring a global threshold.
        val enhanced = Mat()
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(gray, enhanced)

        // Two conservative edge maps are combined: original intensity preserves clean borders,
        // while CLAHE recovers weak ones. HoughLinesP is allowed to bridge sizeable gaps.
        val edgesOriginal = Mat()
        val edgesEnhanced = Mat()
        Imgproc.Canny(gray, edgesOriginal, 20.0, 60.0, 3, true)
        Imgproc.Canny(enhanced, edgesEnhanced, 35.0, 105.0, 3, true)
        val edges = Mat()
        Core.bitwise_or(edgesOriginal, edgesEnhanced, edges)

        val candidates = collectLineCandidates(gray, enhanced, edges)
        if (candidates.size < 4) return null

        val best = findBestQuadrilateral(working, gray, edges, candidates) ?: return null
        val inverseScale = 1.0 / scale
        return best.map { Point(it.x * inverseScale, it.y * inverseScale) }.toTypedArray()
    }

    private fun collectLineCandidates(gray: Mat, enhanced: Mat, edges: Mat): List<LineCandidate> {
        val diagonal = hypot(gray.width().toDouble(), gray.height().toDouble())
        val raw = mutableListOf<LineCandidate>()

        // LSD is especially useful for a faint but locally continuous side.
        val detector = Imgproc.createLineSegmentDetector(Imgproc.LSD_REFINE_STD)
        for (input in listOf(gray, enhanced)) {
            val detected = Mat()
            detector.detect(input, detected)
            for (row in 0 until detected.rows()) {
                val values = detected.get(row, 0) ?: continue
                if (values.size < 4) continue
                addLine(raw, Point(values[0], values[1]), Point(values[2], values[3]), diagonal * 0.10)
            }
        }

        // Probabilistic Hough joins fragments, which is the key distinction from contour finding.
        val hough = Mat()
        Imgproc.HoughLinesP(
            edges,
            hough,
            1.0,
            PI / 360.0,
            24,
            diagonal * 0.12,
            diagonal * 0.07
        )
        for (row in 0 until hough.rows()) {
            val values = hough.get(row, 0) ?: continue
            if (values.size < 4) continue
            addLine(raw, Point(values[0], values[1]), Point(values[2], values[3]), diagonal * 0.10)
        }

        // Collapse near-identical detections so quadrilateral enumeration remains both fast and
        // unbiased by a border that happened to be returned many times.
        val deduplicated = mutableListOf<LineCandidate>()
        for (line in raw.sortedByDescending { it.observedLength }) {
            val duplicate = deduplicated.any { kept ->
                angleDifference(line.angle, kept.angle) < Math.toRadians(3.0) &&
                    (line.distance(kept.midpoint()) + kept.distance(line.midpoint())) / 2.0 < 8.0
            }
            if (!duplicate) deduplicated.add(line)
            if (deduplicated.size >= 55) break
        }
        return deduplicated
    }

    private fun addLine(target: MutableList<LineCandidate>, p1: Point, p2: Point, minLength: Double) {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val length = hypot(dx, dy)
        if (length < minLength) return

        var angle = atan2(dy, dx)
        if (angle < 0.0) angle += PI
        if (angle >= PI) angle -= PI

        // Unit normal for ax + by + c = 0.
        val a = -kotlin.math.sin(angle)
        val b = kotlin.math.cos(angle)
        val c = -(a * p1.x + b * p1.y)
        target.add(LineCandidate(p1, p2, angle, a, b, c, length))
    }

    private fun findBestQuadrilateral(
        colour: Mat,
        gray: Mat,
        edges: Mat,
        lines: List<LineCandidate>
    ): Array<Point>? {
        val width = gray.width().toDouble()
        val height = gray.height().toDouble()
        val diagonal = hypot(width, height)
        val imageArea = width * height

        val oppositePairs = mutableListOf<OppositePair>()
        for (i in 0 until lines.size - 1) {
            for (j in i + 1 until lines.size) {
                val first = lines[i]
                val second = lines[j]
                val angleDelta = angleDifference(first.angle, second.angle)
                // Opposite paper edges can converge under perspective, but a wider difference
                // tends to pair unrelated folds/text lines into a plausible-looking trapezoid.
                if (angleDelta > Math.toRadians(15.0)) continue

                val separation = (first.distance(second.midpoint()) + second.distance(first.midpoint())) / 2.0
                if (separation < diagonal * 0.12) continue

                oppositePairs.add(
                    OppositePair(first, second, averageLineAngle(first.angle, second.angle), separation)
                )
            }
        }

        var bestPoints: Array<Point>? = null
        var bestScore = Double.NEGATIVE_INFINITY
        var tested = 0

        for (i in 0 until oppositePairs.size - 1) {
            val pairA = oppositePairs[i]
            for (j in i + 1 until oppositePairs.size) {
                val pairB = oppositePairs[j]
                val familyAngle = angleDifference(pairA.angle, pairB.angle)
                if (familyAngle < Math.toRadians(42.0) || familyAngle > Math.toRadians(90.0)) continue

                val intersections = arrayOf(
                    intersection(pairA.first, pairB.first) ?: continue,
                    intersection(pairA.first, pairB.second) ?: continue,
                    intersection(pairA.second, pairB.second) ?: continue,
                    intersection(pairA.second, pairB.first) ?: continue
                )
                if (intersections.any { it.x < -width * 0.06 || it.x > width * 1.06 || it.y < -height * 0.06 || it.y > height * 1.06 }) continue

                val ordered = orderPoints(intersections)
                if (!hasFourDistinctPoints(ordered, diagonal * 0.04)) continue
                val polygon = MatOfPoint(*ordered)
                if (!Imgproc.isContourConvex(polygon)) continue

                val area = abs(Imgproc.contourArea(MatOfPoint2f(*ordered)))
                val areaFraction = area / imageArea
                if (areaFraction !in 0.14..0.90) continue
                if (!reasonableCornerAngles(ordered)) continue

                tested++
                val sideSupports = sideEdgeSupports(edges, ordered)
                val edgeSupport = sideSupports.average()
                val weakestSideSupport = sideSupports.minOrNull() ?: 0.0
                val colourContrast = sideColourContrast(colour, ordered)
                val centreBonus = if (Imgproc.pointPolygonTest(MatOfPoint2f(*ordered), Point(width / 2.0, height / 2.0), false) >= 0.0) 1.0 else 0.0
                val polygonCentre = Point(ordered.map { it.x }.average(), ordered.map { it.y }.average())
                val centreDistance = hypot(polygonCentre.x - width / 2.0, polygonCentre.y - height / 2.0) / diagonal
                val centreAlignment = (1.0 - centreDistance * 3.0).coerceIn(0.0, 1.0)
                val observedRatio = min(
                    1.0,
                    (pairA.first.observedLength + pairA.second.observedLength + pairB.first.observedLength + pairB.second.observedLength) /
                        polygonPerimeter(ordered)
                )

                // Edge/contrast dominate; area is a useful prior but cannot by itself win.
                val score = edgeSupport * 3.8 + weakestSideSupport * 1.8 + colourContrast * 2.0 +
                    observedRatio * 1.2 + areaFraction * 1.4 + centreBonus * 0.2 + centreAlignment * 1.1
                if (score > bestScore) {
                    bestScore = score
                    bestPoints = ordered
                }
            }
        }

        // A weak quadrilateral is worse than returning "not detected" and inviting manual crop.
        return if (tested > 0 && bestScore >= 2.25) bestPoints else null
    }

    private fun intersection(first: LineCandidate, second: LineCandidate): Point? {
        val determinant = first.a * second.b - second.a * first.b
        if (abs(determinant) < 1e-5) return null
        val x = (first.b * second.c - second.b * first.c) / determinant
        val y = (first.c * second.a - second.c * first.a) / determinant
        return Point(x, y)
    }

    private fun sideEdgeSupports(edges: Mat, points: Array<Point>): List<Double> {
        val result = mutableListOf<Double>()
        for (side in points.indices) {
            var supported = 0
            var samples = 0
            val start = points[side]
            val end = points[(side + 1) % points.size]
            val count = max(12, (hypot(end.x - start.x, end.y - start.y) / 4.0).toInt())
            for (step in 0..count) {
                val t = step.toDouble() / count
                val x = (start.x + (end.x - start.x) * t).toInt()
                val y = (start.y + (end.y - start.y) * t).toInt()
                var hit = false
                for (dy in -3..3) {
                    for (dx in -3..3) {
                        val px = x + dx
                        val py = y + dy
                        if (px in 0 until edges.cols() && py in 0 until edges.rows() && (edges.get(py, px)?.get(0) ?: 0.0) > 0.0) {
                            hit = true
                            break
                        }
                    }
                    if (hit) break
                }
                if (hit) supported++
                samples++
            }
            result.add(if (samples == 0) 0.0 else supported.toDouble() / samples)
        }
        return result
    }

    private fun sideColourContrast(image: Mat, points: Array<Point>): Double {
        var accumulated = 0.0
        var samples = 0
        val centre = Point(points.map { it.x }.average(), points.map { it.y }.average())

        for (side in points.indices) {
            val start = points[side]
            val end = points[(side + 1) % points.size]
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = hypot(dx, dy)
            if (length < 1.0) continue
            var nx = -dy / length
            var ny = dx / length
            val middle = Point((start.x + end.x) / 2.0, (start.y + end.y) / 2.0)
            if ((centre.x - middle.x) * nx + (centre.y - middle.y) * ny < 0.0) {
                nx = -nx
                ny = -ny
            }

            for (step in 2..10) {
                val t = step / 12.0
                val x = start.x + dx * t
                val y = start.y + dy * t
                val inside = sampleBgr(image, x + nx * 5.0, y + ny * 5.0) ?: continue
                val outside = sampleBgr(image, x - nx * 5.0, y - ny * 5.0) ?: continue
                val difference = (abs(inside[0] - outside[0]) + abs(inside[1] - outside[1]) + abs(inside[2] - outside[2])) / (3.0 * 255.0)
                accumulated += min(1.0, difference * 4.0)
                samples++
            }
        }
        return if (samples == 0) 0.0 else accumulated / samples
    }

    private fun sampleBgr(image: Mat, x: Double, y: Double): DoubleArray? {
        val px = x.toInt()
        val py = y.toInt()
        if (px !in 0 until image.cols() || py !in 0 until image.rows()) return null
        return image.get(py, px)
    }

    private fun reasonableCornerAngles(points: Array<Point>): Boolean {
        for (i in points.indices) {
            val previous = points[(i + points.size - 1) % points.size]
            val current = points[i]
            val next = points[(i + 1) % points.size]
            val ux = previous.x - current.x
            val uy = previous.y - current.y
            val vx = next.x - current.x
            val vy = next.y - current.y
            val denominator = hypot(ux, uy) * hypot(vx, vy)
            if (denominator < 1e-6) return false
            val cosine = ((ux * vx + uy * vy) / denominator).coerceIn(-1.0, 1.0)
            val angle = Math.toDegrees(kotlin.math.acos(cosine))
            if (angle !in 42.0..138.0) return false
        }
        return true
    }

    private fun hasFourDistinctPoints(points: Array<Point>, minDistance: Double): Boolean {
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                if (hypot(points[i].x - points[j].x, points[i].y - points[j].y) < minDistance) return false
            }
        }
        return true
    }

    private fun polygonPerimeter(points: Array<Point>): Double = points.indices.sumOf { index ->
        val next = points[(index + 1) % points.size]
        hypot(next.x - points[index].x, next.y - points[index].y)
    }

    private fun angleDifference(first: Double, second: Double): Double {
        val raw = abs(first - second) % PI
        return min(raw, PI - raw)
    }

    private fun averageLineAngle(first: Double, second: Double): Double {
        // Doubling angles makes the mean invariant to a line's 180-degree direction.
        val x = kotlin.math.cos(first * 2.0) + kotlin.math.cos(second * 2.0)
        val y = kotlin.math.sin(first * 2.0) + kotlin.math.sin(second * 2.0)
        var result = atan2(y, x) / 2.0
        if (result < 0.0) result += PI
        return result
    }

    fun cropWarped(source: Mat, corners: Array<Point>): Mat {
        require(corners.size == 4) { "Exactly four document corners are required" }
        val tl = corners[0]
        val tr = corners[1]
        val br = corners[2]
        val bl = corners[3]

        val maxWidth = max(hypot(br.x - bl.x, br.y - bl.y), hypot(tr.x - tl.x, tr.y - tl.y)).toInt()
        val maxHeight = max(hypot(tr.x - br.x, tr.y - br.y), hypot(tl.x - bl.x, tl.y - bl.y)).toInt()
        require(maxWidth > 0 && maxHeight > 0) { "Document corners form an empty crop" }

        val destination = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )
        val transform = Imgproc.getPerspectiveTransform(MatOfPoint2f(*corners), destination)
        val warped = Mat()
        Imgproc.warpPerspective(source, warped, transform, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        return warped
    }

    fun detectAndCrop(source: Mat): Mat? = detectCorners(source)?.let { cropWarped(source, it) }

    private fun orderPoints(points: Array<Point>): Array<Point> {
        val centreX = points.map { it.x }.average()
        val centreY = points.map { it.y }.average()
        val clockwise = points.sortedBy { atan2(it.y - centreY, it.x - centreX) }
        val topLeftIndex = clockwise.indices.minByOrNull { clockwise[it].x + clockwise[it].y } ?: 0
        return Array(4) { clockwise[(topLeftIndex + it) % 4] }
    }
}

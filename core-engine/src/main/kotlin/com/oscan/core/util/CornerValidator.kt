package com.oscan.core.util

import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Validates quadrilateral geometry for document scanning corner handles.
 */
object CornerValidator {

    /**
     * Orders four unorganized points into Top-Left, Top-Right, Bottom-Right, Bottom-Left order.
     */
    fun orderPoints(points: Array<Point>): Array<Point> {
        require(points.size == 4) { "Must provide exactly 4 points" }
        val centreX = points.map { it.x }.average()
        val centreY = points.map { it.y }.average()
        val clockwise = points.sortedBy { atan2(it.y - centreY, it.x - centreX) }
        val topLeftIndex = clockwise.indices.minByOrNull { clockwise[it].x + clockwise[it].y } ?: 0
        return Array(4) { clockwise[(topLeftIndex + it) % 4] }
    }

    /**
     * Checks if four corner points form a strictly convex polygon with positive area.
     */
    fun isConvex(corners: Array<Point>): Boolean {
        if (corners.size != 4) return false
        var sign = 0.0
        for (i in corners.indices) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            val c = corners[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (abs(cross) < 1e-6) return false
            if (sign == 0.0) sign = sign(cross) else if (sign(cross) != sign) return false
        }
        return true
    }

    /**
     * Checks if all corner internal angles fall within acceptable threshold (default 42°..138°).
     */
    fun reasonableCornerAngles(corners: Array<Point>, minDegrees: Double = 35.0, maxDegrees: Double = 145.0): Boolean {
        if (corners.size != 4) return false
        for (i in corners.indices) {
            val previous = corners[(i + corners.size - 1) % corners.size]
            val current = corners[i]
            val next = corners[(i + 1) % corners.size]
            val ux = previous.x - current.x
            val uy = previous.y - current.y
            val vx = next.x - current.x
            val vy = next.y - current.y
            val denominator = hypot(ux, uy) * hypot(vx, vy)
            if (denominator < 1e-6) return false
            val cosine = ((ux * vx + uy * vy) / denominator).coerceIn(-1.0, 1.0)
            val angle = Math.toDegrees(acos(cosine))
            if (angle !in minDegrees..maxDegrees) return false
        }
        return true
    }

    /**
     * Checks if all distinct points are separated by at least [minDistance].
     */
    fun hasDistinctPoints(corners: Array<Point>, minDistance: Double = 10.0): Boolean {
        if (corners.size != 4) return false
        for (i in corners.indices) {
            for (j in i + 1 until corners.size) {
                if (hypot(corners[i].x - corners[j].x, corners[i].y - corners[j].y) < minDistance) return false
            }
        }
        return true
    }

    /**
     * Clamps a single point to within image boundaries.
     */
    fun clampPoint(point: Point, dimensions: ImageDimensions): Point {
        val clampedX = point.x.coerceIn(0.0, dimensions.width.toDouble())
        val clampedY = point.y.coerceIn(0.0, dimensions.height.toDouble())
        return Point(clampedX, clampedY)
    }

    /**
     * Comprehensive validity check for candidate document corners.
     */
    fun isValidQuadrilateral(corners: Array<Point>, dimensions: ImageDimensions? = null): Boolean {
        if (corners.size != 4) return false
        if (!hasDistinctPoints(corners)) return false
        if (!isConvex(corners)) return false
        if (!reasonableCornerAngles(corners)) return false
        if (dimensions != null) {
            val marginX = dimensions.width * 0.15
            val marginY = dimensions.height * 0.15
            if (corners.any {
                    it.x < -marginX || it.x > dimensions.width + marginX ||
                    it.y < -marginY || it.y > dimensions.height + marginY
                }) return false
        }
        return true
    }
}

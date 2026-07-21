package com.oscan.core.util

import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point
import kotlin.math.min

/**
 * Handles bidirectional coordinate mapping between source image pixels and display container layout.
 */
object CoordinateTransformer {

    data class DisplayTransform(
        val scale: Double,
        val offsetX: Double,
        val offsetY: Double,
        val renderedWidth: Double,
        val renderedHeight: Double
    )

    /**
     * Computes the aspect-fit display transformation matrix parameters.
     */
    fun computeTransform(source: ImageDimensions, container: ImageDimensions): DisplayTransform {
        if (source.width <= 0 || source.height <= 0 || container.width <= 0 || container.height <= 0) {
            return DisplayTransform(1.0, 0.0, 0.0, container.width.toDouble(), container.height.toDouble())
        }

        val scale = min(
            container.width.toDouble() / source.width.toDouble(),
            container.height.toDouble() / source.height.toDouble()
        )
        val renderedWidth = source.width * scale
        val renderedHeight = source.height * scale
        val offsetX = (container.width - renderedWidth) / 2.0
        val offsetY = (container.height - renderedHeight) / 2.0

        return DisplayTransform(scale, offsetX, offsetY, renderedWidth, renderedHeight)
    }

    /**
     * Maps a source-image point to display container coordinates.
     */
    fun sourceToDisplay(sourcePoint: Point, transform: DisplayTransform): Point {
        val x = transform.offsetX + sourcePoint.x * transform.scale
        val y = transform.offsetY + sourcePoint.y * transform.scale
        return Point(x, y)
    }

    /**
     * Maps a display container point back to source-image coordinates, clamping within image bounds.
     */
    fun displayToSource(displayPoint: Point, transform: DisplayTransform, source: ImageDimensions): Point {
        val rawX = (displayPoint.x - transform.offsetX) / transform.scale
        val rawY = (displayPoint.y - transform.offsetY) / transform.scale
        val clampedX = rawX.coerceIn(0.0, source.width.toDouble())
        val clampedY = rawY.coerceIn(0.0, source.height.toDouble())
        return Point(clampedX, clampedY)
    }

    /**
     * Maps all four corner points from source to display coordinates.
     */
    fun cornersToDisplay(corners: CornerPoints, transform: DisplayTransform): CornerPoints {
        return CornerPoints(
            topLeft = sourceToDisplay(corners.topLeft, transform),
            topRight = sourceToDisplay(corners.topRight, transform),
            bottomRight = sourceToDisplay(corners.bottomRight, transform),
            bottomLeft = sourceToDisplay(corners.bottomLeft, transform)
        )
    }

    /**
     * Maps all four corner points from display to source coordinates.
     */
    fun cornersToSource(corners: CornerPoints, transform: DisplayTransform, source: ImageDimensions): CornerPoints {
        return CornerPoints(
            topLeft = displayToSource(corners.topLeft, transform, source),
            topRight = displayToSource(corners.topRight, transform, source),
            bottomRight = displayToSource(corners.bottomRight, transform, source),
            bottomLeft = displayToSource(corners.bottomLeft, transform, source)
        )
    }
}

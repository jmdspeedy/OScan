package com.oscan.core.model

import org.opencv.core.Point

/**
 * Represents the 2D dimensions of a source or display image.
 *
 * @property width Image width in pixels.
 * @property height Image height in pixels.
 */
data class ImageDimensions(val width: Int, val height: Int) {
    val aspectRatio: Double get() = if (height == 0) 1.0 else width.toDouble() / height.toDouble()
}

/**
 * Represents the four corner points of a detected or user-adjusted document boundary.
 *
 * @property topLeft Top-left corner in source-image pixel coordinates.
 * @property topRight Top-right corner in source-image pixel coordinates.
 * @property bottomRight Bottom-right corner in source-image pixel coordinates.
 * @property bottomLeft Bottom-left corner in source-image pixel coordinates.
 */
data class CornerPoints(
    val topLeft: Point,
    val topRight: Point,
    val bottomRight: Point,
    val bottomLeft: Point
) {
    fun toArray(): Array<Point> = arrayOf(topLeft, topRight, bottomRight, bottomLeft)

    companion object {
        fun fromArray(array: Array<Point>): CornerPoints {
            require(array.size == 4) { "Array must contain exactly 4 points" }
            return CornerPoints(array[0], array[1], array[2], array[3])
        }

        /**
         * Generates default inset corners (e.g. 10% inset from image boundaries).
         */
        fun defaultInset(width: Int, height: Int, insetFraction: Double = 0.10): CornerPoints {
            val insetX = width * insetFraction
            val insetY = height * insetFraction
            return CornerPoints(
                topLeft = Point(insetX, insetY),
                topRight = Point(width - insetX, insetY),
                bottomRight = Point(width - insetX, height - insetY),
                bottomLeft = Point(insetX, height - insetY)
            )
        }
    }
}

/**
 * Enum defining available image enhancement filters.
 */
enum class FilterType {
    ORIGINAL,
    MAGIC
}

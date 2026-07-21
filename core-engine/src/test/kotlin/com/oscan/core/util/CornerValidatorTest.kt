package com.oscan.core.util

import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CornerValidatorTest {

    @Test
    fun testValidConvexQuadrilateral() {
        val validCorners = arrayOf(
            Point(100.0, 100.0),
            Point(900.0, 120.0),
            Point(880.0, 950.0),
            Point(80.0, 920.0)
        )
        val dimensions = ImageDimensions(1000, 1000)

        assertTrue(CornerValidator.isConvex(validCorners))
        assertTrue(CornerValidator.hasDistinctPoints(validCorners))
        assertTrue(CornerValidator.reasonableCornerAngles(validCorners))
        assertTrue(CornerValidator.isValidQuadrilateral(validCorners, dimensions))
    }

    @Test
    fun testSelfIntersectingQuadrilateralRejected() {
        // Hourglass / bow-tie self-intersecting polygon
        val invalidCorners = arrayOf(
            Point(100.0, 100.0),
            Point(880.0, 950.0), // Swapped TR and BR
            Point(900.0, 120.0),
            Point(80.0, 920.0)
        )

        assertFalse(CornerValidator.isConvex(invalidCorners))
        assertFalse(CornerValidator.isValidQuadrilateral(invalidCorners))
    }

    @Test
    fun testNonDistinctCornersRejected() {
        val collapsedCorners = arrayOf(
            Point(100.0, 100.0),
            Point(100.0, 100.0), // Duplicate point
            Point(900.0, 900.0),
            Point(100.0, 900.0)
        )

        assertFalse(CornerValidator.hasDistinctPoints(collapsedCorners, minDistance = 10.0))
        assertFalse(CornerValidator.isValidQuadrilateral(collapsedCorners))
    }

    @Test
    fun testExtremeAnglesRejected() {
        // Nearly collinear triangle-like corner
        val acuteCorners = arrayOf(
            Point(100.0, 100.0),
            Point(900.0, 100.0),
            Point(110.0, 105.0), // Very sharp acute corner angle
            Point(100.0, 900.0)
        )

        assertFalse(CornerValidator.reasonableCornerAngles(acuteCorners))
        assertFalse(CornerValidator.isValidQuadrilateral(acuteCorners))
    }
}

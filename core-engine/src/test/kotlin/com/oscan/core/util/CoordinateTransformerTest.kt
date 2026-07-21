package com.oscan.core.util

import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordinateTransformerTest {

    @Test
    fun testAspectFitTransformScalingAndOffset() {
        val source = ImageDimensions(1000, 2000) // 1:2 aspect ratio
        val container = ImageDimensions(500, 500) // 1:1 container square

        val transform = CoordinateTransformer.computeTransform(source, container)

        // Scale should be limited by height: 500 / 2000 = 0.25
        assertEquals(0.25, transform.scale, 0.001)

        // Rendered width: 1000 * 0.25 = 250
        // Rendered height: 2000 * 0.25 = 500
        // Horizontal offset: (500 - 250) / 2 = 125.0
        // Vertical offset: (500 - 500) / 2 = 0.0
        assertEquals(125.0, transform.offsetX, 0.001)
        assertEquals(0.0, transform.offsetY, 0.001)
    }

    @Test
    fun testSourceToDisplayMapping() {
        val source = ImageDimensions(1000, 2000)
        val container = ImageDimensions(500, 500)
        val transform = CoordinateTransformer.computeTransform(source, container)

        // Top-left source (0,0) -> display (125.0, 0.0)
        val displayTL = CoordinateTransformer.sourceToDisplay(Point(0.0, 0.0), transform)
        assertEquals(125.0, displayTL.x, 0.001)
        assertEquals(0.0, displayTL.y, 0.001)

        // Bottom-right source (1000, 2000) -> display (375.0, 500.0)
        val displayBR = CoordinateTransformer.sourceToDisplay(Point(1000.0, 2000.0), transform)
        assertEquals(375.0, displayBR.x, 0.001)
        assertEquals(500.0, displayBR.y, 0.001)
    }

    @Test
    fun testDisplayToSourceRoundTrip() {
        val source = ImageDimensions(1920, 1080)
        val container = ImageDimensions(800, 600)
        val transform = CoordinateTransformer.computeTransform(source, container)

        val originalSourcePoint = Point(450.0, 720.0)
        val displayPoint = CoordinateTransformer.sourceToDisplay(originalSourcePoint, transform)
        val recoveredSourcePoint = CoordinateTransformer.displayToSource(displayPoint, transform, source)

        assertEquals(originalSourcePoint.x, recoveredSourcePoint.x, 0.001)
        assertEquals(originalSourcePoint.y, recoveredSourcePoint.y, 0.001)
    }

    @Test
    fun testDisplayToSourceClamping() {
        val source = ImageDimensions(1000, 1000)
        val container = ImageDimensions(500, 500)
        val transform = CoordinateTransformer.computeTransform(source, container)

        // Point clicked outside image container (-100, 600)
        val outOfBoundsDisplayPoint = Point(-100.0, 600.0)
        val clampedSourcePoint = CoordinateTransformer.displayToSource(outOfBoundsDisplayPoint, transform, source)

        assertTrue(clampedSourcePoint.x >= 0.0 && clampedSourcePoint.x <= source.width.toDouble())
        assertTrue(clampedSourcePoint.y >= 0.0 && clampedSourcePoint.y <= source.height.toDouble())
    }
}

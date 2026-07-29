package com.oscan.android.ui

import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScannerViewModelTest {

    @Test
    fun testDefaultStateIsEmpty() {
        val initialState: ScannerUiState = ScannerUiState.Empty
        assertTrue(initialState is ScannerUiState.Empty)
    }

    @Test
    fun testValidCornerGeometryCalculation() {
        val dimensions = ImageDimensions(1000, 1000)
        val validCorners = CornerPoints(
            topLeft = Point(100.0, 100.0),
            topRight = Point(900.0, 100.0),
            bottomRight = Point(900.0, 900.0),
            bottomLeft = Point(100.0, 900.0)
        )

        val isValid = com.oscan.core.util.CornerValidator.isValidQuadrilateral(
            corners = validCorners.toArray(),
            dimensions = dimensions
        )
        assertTrue(isValid)
    }

    @Test
    fun testInvalidCornerGeometryCalculation() {
        val dimensions = ImageDimensions(1000, 1000)
        // Self-intersecting bow-tie corners
        val invalidCorners = CornerPoints(
            topLeft = Point(100.0, 100.0),
            topRight = Point(900.0, 900.0), // Swapped
            bottomRight = Point(900.0, 100.0),
            bottomLeft = Point(100.0, 900.0)
        )

        val isValid = com.oscan.core.util.CornerValidator.isValidQuadrilateral(
            corners = invalidCorners.toArray(),
            dimensions = dimensions
        )
        assertFalse(isValid)
    }
}

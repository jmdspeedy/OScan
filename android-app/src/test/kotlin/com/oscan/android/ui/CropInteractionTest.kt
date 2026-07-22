package com.oscan.android.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.opencv.core.Point

class CropInteractionTest {
    private val rectangle = listOf(
        Point(20.0, 20.0),
        Point(180.0, 20.0),
        Point(180.0, 280.0),
        Point(20.0, 280.0)
    )

    @Test
    fun cornerWinsWhenTouchIsNearCornerAndEdge() {
        assertEquals(
            CropDragTarget.Corner(0),
            findCropDragTarget(rectangle, Offset(24f, 24f), cornerThreshold = 30f, edgeThreshold = 20f)
        )
    }

    @Test
    fun fullEdgeIsDirectlyDraggable() {
        assertEquals(
            CropDragTarget.Edge(1),
            findCropDragTarget(rectangle, Offset(174f, 150f), cornerThreshold = 24f, edgeThreshold = 16f)
        )
    }

    @Test
    fun touchAwayFromBoundaryDoesNotStartDrag() {
        assertNull(
            findCropDragTarget(rectangle, Offset(100f, 140f), cornerThreshold = 24f, edgeThreshold = 16f)
        )
    }

    @Test
    fun sustainedSlowMovementAccumulatesTowardPrecisionMode() {
        val accumulated = updatedPrecisionSlowMillis(
            currentSlowMillis = 120L,
            distancePx = 3f,
            density = 3f,
            elapsedMillis = 60L
        )

        assertEquals(180L, accumulated)
    }

    @Test
    fun fastMovementResetsPrecisionIntent() {
        val accumulated = updatedPrecisionSlowMillis(
            currentSlowMillis = 160L,
            distancePx = 40f,
            density = 2f,
            elapsedMillis = 20L
        )

        assertEquals(0L, accumulated)
    }

    @Test
    fun loupeStaysInsideWorkspaceOnEitherSide() {
        val container = IntSize(900, 1200)
        val leftFocusPosition = loupeTopLeft(container, Offset(40f, 50f), 420f, 510f, 42f)
        val rightFocusPosition = loupeTopLeft(container, Offset(860f, 50f), 420f, 510f, 42f)

        assertEquals(438f, leftFocusPosition.x)
        assertEquals(42f, leftFocusPosition.y)
        assertEquals(42f, rightFocusPosition.x)
        assertEquals(42f, rightFocusPosition.y)
    }

    @Test
    fun loupePositionIsClampedForSmallWorkspace() {
        val position = loupeTopLeft(
            containerSize = IntSize(300, 400),
            focus = Offset(20f, 20f),
            loupeWidth = 420f,
            loupeHeight = 510f,
            margin = 42f
        )

        assertEquals(0f, position.x)
        assertEquals(0f, position.y)
    }

    @Test
    fun edgeFocusRemainsUnderLoupeCrosshair() {
        val viewport = IntSize(420, 336)
        val previewScale = 0.75f
        val zoom = 5f
        val focus = Point(0.0, 300.0)

        val crop = calculateLoupeCrop(
            bitmapSize = IntSize(600, 800),
            viewportSize = viewport,
            focus = focus,
            previewScale = previewScale,
            zoom = zoom
        )
        val renderedFocusX = crop.destinationOffset.x +
            (focus.x - crop.sourceOffset.x) * previewScale * zoom
        val renderedFocusY = crop.destinationOffset.y +
            (focus.y - crop.sourceOffset.y) * previewScale * zoom

        assertTrue(kotlin.math.abs(renderedFocusX - viewport.width / 2f) <= 1f)
        assertTrue(kotlin.math.abs(renderedFocusY - viewport.height / 2f) <= 1f)
    }
}

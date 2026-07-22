package com.oscan.android.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}

package com.oscan.android.engine

import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.opencv.core.Point

class IdCardDetectionPolicyTest {
    private val dimensions = ImageDimensions(1000, 700)

    @Test
    fun detectedEdgesTakePriorityOverGuide() {
        val detected = arrayOf(
            Point(43.0, 76.0),
            Point(925.0, 51.0),
            Point(947.0, 617.0),
            Point(61.0, 638.0)
        )

        val (corners, autoDetected) = idCardCornersOrGuide(detected, dimensions)

        assertTrue(autoDetected)
        assertEquals(CornerPoints.fromArray(detected), corners)
    }

    @Test
    fun guideIsOnlyUsedWhenFourEdgesWereNotDetected() {
        val (corners, autoDetected) = idCardCornersOrGuide(null, dimensions)

        assertFalse(autoDetected)
        assertEquals(idCardGuideCorners(dimensions), corners)
    }

    @Test
    fun largeCameraCapturesAreSampledBeforeDetection() {
        assertEquals(4, detectionSampleSize(width = 4032, height = 3024, maxDimension = 1200))
        assertEquals(1, detectionSampleSize(width = 1080, height = 720, maxDimension = 1200))
    }
}

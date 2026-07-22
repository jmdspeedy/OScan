package com.oscan.core

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.opencv.core.Point

class DocumentScannerWarpTest {
    private val scanner = DocumentScanner()

    @Test
    fun portraitSheetUsesClosestCommonPaperAspect() {
        val corrected = scanner.correctedDocumentAspect(measuredRatio = 1.03, boundsRatio = .70)
        assertEquals(1.0 / sqrt(2.0), corrected, .001)
    }

    @Test
    fun narrowReceiptKeepsMeasuredAspect() {
        val corrected = scanner.correctedDocumentAspect(measuredRatio = .31, boundsRatio = .30)
        assertEquals(.31, corrected, .001)
    }

    @Test
    fun landscapeSheetProducesLandscapeOutput() {
        val corrected = scanner.correctedDocumentAspect(measuredRatio = .96, boundsRatio = 1.42)
        assertEquals(sqrt(2.0), corrected, .001)
    }

    @Test
    fun obliqueViewRecoversPhysicalPageAspect() {
        val corners = arrayOf(
            Point(477.3, 438.3),
            Point(2534.2, 365.5),
            Point(2860.0, 3729.5),
            Point(8.0, 3577.7)
        )

        val estimated = assertNotNull(scanner.estimateProjectiveAspect(corners, 3072, 4096))
        assertEquals(.723, estimated, .01)
        assertEquals(
            1.0 / sqrt(2.0),
            scanner.correctedDocumentAspect(1.0, .848, estimated),
            .001
        )
    }
}

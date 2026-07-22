package com.oscan.core

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

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
}

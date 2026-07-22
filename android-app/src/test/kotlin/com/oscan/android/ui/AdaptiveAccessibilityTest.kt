package com.oscan.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveAccessibilityTest {
    @Test
    fun widthClassUsesMaterialAdaptiveBreakpoints() {
        assertEquals(OScanWindowWidthClass.COMPACT, oscanWindowWidthClass(320))
        assertEquals(OScanWindowWidthClass.COMPACT, oscanWindowWidthClass(599))
        assertEquals(OScanWindowWidthClass.MEDIUM, oscanWindowWidthClass(600))
        assertEquals(OScanWindowWidthClass.MEDIUM, oscanWindowWidthClass(839))
        assertEquals(OScanWindowWidthClass.EXPANDED, oscanWindowWidthClass(840))
        assertEquals(OScanWindowWidthClass.EXPANDED, oscanWindowWidthClass(1280))
    }
}

package com.oscan.core

import com.oscan.core.model.FilterType
import nu.pattern.OpenCV
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageEnhancerTest {
    @BeforeTest
    fun loadOpenCv() = OpenCV.loadLocally()

    @Test
    fun magicPreservesColourAndResolution() {
        val source = Mat(180, 240, CvType.CV_8UC3, Scalar(205.0, 205.0, 205.0))
        source.submat(Rect(30, 30, 70, 70)).setTo(Scalar(180.0, 80.0, 30.0))

        val result = ImageEnhancer().applyFilter(source, FilterType.MAGIC)

        assertEquals(source.size(), result.size())
        assertEquals(3, result.channels())
        val colour = result.get(60, 60)
        assertTrue(kotlin.math.abs(colour[0] - colour[2]) > 20.0)
        result.release()
        source.release()
    }

    @Test
    fun blackWhiteIsSingleChannelWithoutResizing() {
        val source = Mat(180, 240, CvType.CV_8UC3, Scalar(220.0, 220.0, 220.0))
        val result = ImageEnhancer().applyFilter(source, FilterType.BLACK_WHITE)
        assertEquals(source.size(), result.size())
        assertEquals(1, result.channels())
        result.release()
        source.release()
    }
}

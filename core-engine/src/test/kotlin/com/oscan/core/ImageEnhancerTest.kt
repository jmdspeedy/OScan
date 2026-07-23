package com.oscan.core

import com.oscan.core.model.FilterType
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
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

    @Test
    fun magicFlattensBrightNeutralPaperNoise() {
        val source = Mat(180, 240, CvType.CV_8UC3)
        for (row in 0 until source.rows()) {
            for (column in 0 until source.cols()) {
                val paper = 215.0 + ((row * 13 + column * 17) % 11)
                source.put(row, column, paper, paper, paper)
            }
        }

        val result = ImageEnhancer().applyFilter(source, FilterType.MAGIC)
        val gray = Mat()
        Imgproc.cvtColor(result, gray, Imgproc.COLOR_BGR2GRAY)
        val mean = MatOfDouble()
        val deviation = MatOfDouble()
        Core.meanStdDev(gray, mean, deviation)

        assertTrue(mean.toArray().single() > 248.0)
        assertTrue(deviation.toArray().single() < 2.0)

        mean.release()
        deviation.release()
        gray.release()
        result.release()
        source.release()
    }

    @Test
    fun grayscaleFlattensBrightPaperNoise() {
        val source = noisyPaperSource()
        val result = ImageEnhancer().applyFilter(source, FilterType.GRAYSCALE)
        assertUniformWhitePaper(result)
        result.release()
        source.release()
    }

    private fun noisyPaperSource(): Mat {
        val source = Mat(180, 240, CvType.CV_8UC3)
        for (row in 0 until source.rows()) {
            for (column in 0 until source.cols()) {
                val paper = 215.0 + ((row * 13 + column * 17) % 11)
                source.put(row, column, paper, paper, paper)
            }
        }
        return source
    }

    private fun assertUniformWhitePaper(result: Mat) {
        val gray = Mat()
        if (result.channels() == 1) result.copyTo(gray) else Imgproc.cvtColor(result, gray, Imgproc.COLOR_BGR2GRAY)
        val mean = MatOfDouble()
        val deviation = MatOfDouble()
        Core.meanStdDev(gray, mean, deviation)
        assertTrue(mean.toArray().single() > 248.0)
        assertTrue(deviation.toArray().single() < 2.0)
        mean.release()
        deviation.release()
        gray.release()
    }
}

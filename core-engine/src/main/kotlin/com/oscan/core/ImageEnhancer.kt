package com.oscan.core

import com.oscan.core.model.FilterType
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.min
import kotlin.math.roundToInt

class ImageEnhancer {
    fun applyFilter(source: Mat, filter: FilterType): Mat = when (filter) {
        FilterType.ORIGINAL -> source.clone()
        FilterType.MAGIC -> applyMagicFilter(source)
        FilterType.GRAYSCALE -> applyGrayscaleFilter(source)
        FilterType.BLACK_WHITE -> applyBlackWhiteFilter(source)
    }

    /**
     * CamScanner-style colour enhancement. The luminance channel is flattened independently so
     * shadows and paper gradients disappear without throwing away stamps, logos, or table fills.
     */
    fun applyMagicFilter(source: Mat): Mat {
        require(!source.empty()) { "Cannot enhance an empty image" }
        val lab = Mat()
        Imgproc.cvtColor(source, lab, Imgproc.COLOR_BGR2Lab)
        val channels = ArrayList<Mat>(3)
        Core.split(lab, channels)

        val luminance = normalizeIllumination(channels[0])
        val contrast = Mat()
        Imgproc.createCLAHE(1.15, Size(10.0, 10.0)).apply(luminance, contrast)
        contrast.convertTo(contrast, -1, 1.04, -5.0)

        // Flatten only bright, nearly neutral pixels. A small median pass removes sensor/paper
        // texture before the nonlinear shoulder maps paper to solid white. Dark neutral ink and
        // chromatic regions such as stamps, logos, and table fills stay outside this mask.
        val paperMask = createPaperMask(contrast, channels[1], channels[2])
        whitenPaper(contrast, paperMask)

        // Restore modest colour saturation after whitening the luminance channel.
        channels[1].convertTo(channels[1], -1, 1.15, -19.2)
        channels[2].convertTo(channels[2], -1, 1.15, -19.2)
        channels[0].release()
        channels[0] = contrast
        Core.merge(channels, lab)

        val colour = Mat()
        Imgproc.cvtColor(lab, colour, Imgproc.COLOR_Lab2BGR)
        val blurred = Mat()
        Imgproc.GaussianBlur(colour, blurred, Size(0.0, 0.0), 0.8)
        val sharpened = Mat()
        Core.addWeighted(colour, 1.28, blurred, -0.28, 0.0, sharpened)

        luminance.release()
        paperMask.release()
        blurred.release()
        colour.release()
        lab.release()
        channels.forEach(Mat::release)
        return sharpened
    }

    fun applyGrayscaleFilter(source: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY)
        val normalized = normalizeIllumination(gray)
        val enhanced = Mat()
        Imgproc.createCLAHE(1.15, Size(10.0, 10.0)).apply(normalized, enhanced)
        enhanced.convertTo(enhanced, -1, 1.04, -5.0)
        // Although the result is grayscale, source chroma still tells us which bright regions are
        // coloured graphics rather than paper. That prevents blue/red fills from being patchily
        // whitened while keeping the neutral page background clean.
        val sourceLab = Mat()
        Imgproc.cvtColor(source, sourceLab, Imgproc.COLOR_BGR2Lab)
        val sourceChannels = ArrayList<Mat>(3)
        Core.split(sourceLab, sourceChannels)
        val paperMask = createPaperMask(enhanced, sourceChannels[1], sourceChannels[2])
        whitenPaper(enhanced, paperMask)
        val blurred = Mat()
        Imgproc.GaussianBlur(enhanced, blurred, Size(0.0, 0.0), 0.8)
        val sharpened = Mat()
        Core.addWeighted(enhanced, 1.25, blurred, -0.25, 0.0, sharpened)
        gray.release()
        normalized.release()
        paperMask.release()
        sourceLab.release()
        sourceChannels.forEach(Mat::release)
        enhanced.release()
        blurred.release()
        return sharpened
    }

    fun applyBlackWhiteFilter(source: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY)
        val denoised = Mat()
        Imgproc.medianBlur(gray, denoised, 3)
        val normalized = normalizeIllumination(denoised)
        val adaptive = Mat()
        val blockSize = ((min(source.width(), source.height()) / 55) or 1).coerceIn(21, 81)
        Imgproc.adaptiveThreshold(
            normalized,
            adaptive,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            blockSize,
            13.0
        )

        // Adaptive thresholding alone interprets paper grain and sensor noise as tiny ink marks.
        // Gate it with a conservative global threshold: a pixel is black only when it is both
        // locally ink-like and meaningfully darker than normalized paper.
        val global = Mat()
        val otsuThreshold = Imgproc.threshold(
            normalized,
            global,
            0.0,
            255.0,
            Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
        )
        Imgproc.threshold(
            normalized,
            global,
            (otsuThreshold - 8.0).coerceIn(120.0, 205.0),
            255.0,
            Imgproc.THRESH_BINARY
        )
        val binary = Mat()
        Core.bitwise_or(adaptive, global, binary)

        gray.release()
        denoised.release()
        normalized.release()
        adaptive.release()
        global.release()
        return binary
    }

    private fun normalizeIllumination(luminance: Mat): Mat {
        val background = Mat()
        val sigma = (min(luminance.width(), luminance.height()) / 32.0).coerceIn(12.0, 72.0)
        Imgproc.GaussianBlur(luminance, background, Size(0.0, 0.0), sigma)
        val normalized = Mat()
        Core.divide(luminance, background, normalized, 242.0)
        background.release()
        return normalized
    }

    private fun createPaperMask(luminance: Mat, aChannel: Mat, bChannel: Mat): Mat {
        val bright = createBrightPaperMask(luminance)

        val aDistance = Mat()
        val bDistance = Mat()
        val chromaDistance = Mat()
        Core.absdiff(aChannel, Scalar(128.0), aDistance)
        Core.absdiff(bChannel, Scalar(128.0), bDistance)
        Core.max(aDistance, bDistance, chromaDistance)
        val neutral = Mat()
        Imgproc.threshold(chromaDistance, neutral, 14.0, 255.0, Imgproc.THRESH_BINARY_INV)

        val mask = Mat()
        Core.bitwise_and(bright, neutral, mask)
        bright.release()
        aDistance.release()
        bDistance.release()
        chromaDistance.release()
        neutral.release()
        return mask
    }

    private fun createBrightPaperMask(luminance: Mat): Mat {
        val bright = Mat()
        Imgproc.threshold(luminance, bright, 188.0, 255.0, Imgproc.THRESH_BINARY)
        return bright
    }

    private fun whitenPaper(luminance: Mat, paperMask: Mat) {
        val smoothed = Mat()
        Imgproc.medianBlur(luminance, smoothed, 3)
        val paperWhite = applyPaperWhiteCurve(smoothed)
        paperWhite.copyTo(luminance, paperMask)
        smoothed.release()
        paperWhite.release()
    }

    private fun applyPaperWhiteCurve(luminance: Mat): Mat {
        val values = ByteArray(256) { value ->
            val mapped = if (value < 185) {
                value
            } else {
                val t = ((value - 185) / 50.0).coerceIn(0.0, 1.0)
                (185.0 + 70.0 * (1.0 - (1.0 - t) * (1.0 - t))).roundToInt()
            }
            (if (mapped >= 248) 255 else mapped).coerceIn(0, 255).toByte()
        }
        val lookup = Mat(1, 256, CvType.CV_8UC1)
        lookup.put(0, 0, values)
        val result = Mat()
        Core.LUT(luminance, lookup, result)
        lookup.release()
        return result
    }
}

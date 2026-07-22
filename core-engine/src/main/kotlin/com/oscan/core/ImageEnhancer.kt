package com.oscan.core

import com.oscan.core.model.FilterType
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.min

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
        Imgproc.createCLAHE(1.45, Size(10.0, 10.0)).apply(luminance, contrast)
        // Raise near-white paper toward white while slightly deepening dark ink.
        contrast.convertTo(contrast, -1, 1.08, -10.0)

        // Restore modest colour saturation after whitening the luminance channel.
        channels[1].convertTo(channels[1], -1, 1.10, -12.8)
        channels[2].convertTo(channels[2], -1, 1.10, -12.8)
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
        Imgproc.createCLAHE(1.6, Size(10.0, 10.0)).apply(normalized, enhanced)
        val blurred = Mat()
        Imgproc.GaussianBlur(enhanced, blurred, Size(0.0, 0.0), 0.8)
        val sharpened = Mat()
        Core.addWeighted(enhanced, 1.25, blurred, -0.25, 0.0, sharpened)
        gray.release()
        normalized.release()
        enhanced.release()
        blurred.release()
        return sharpened
    }

    fun applyBlackWhiteFilter(source: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY)
        val normalized = normalizeIllumination(gray)
        val binary = Mat()
        val blockSize = ((min(source.width(), source.height()) / 55) or 1).coerceIn(21, 81)
        Imgproc.adaptiveThreshold(
            normalized,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            blockSize,
            9.0
        )
        gray.release()
        normalized.release()
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
}

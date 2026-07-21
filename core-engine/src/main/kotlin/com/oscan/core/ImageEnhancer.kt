package com.oscan.core

import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ImageEnhancer {
    fun applyMagicFilter(source: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY)
        
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(gray, enhanced)
        
        // Simple thresholding to whiten background and darken text
        val binary = Mat()
        Imgproc.adaptiveThreshold(enhanced, binary, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 21, 10.0)
        
        return binary
    }
}

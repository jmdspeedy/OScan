package com.oscan.android.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.oscan.core.DocumentScanner
import com.oscan.core.ImageEnhancer
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

/**
 * Result wrapper for Uri decoding and corner detection.
 *
 * @property sourceDimensions Original source image width & height.
 * @property corners Detected or fallback corner points in source coordinates.
 * @property isAutoDetected True if ML/classical detector successfully detected corners.
 * @property previewBitmap Scaled bitmap used for UI display.
 */
data class DetectionResult(
    val sourceDimensions: ImageDimensions,
    val corners: CornerPoints,
    val isAutoDetected: Boolean,
    val previewBitmap: Bitmap
)

interface ScannerEngine {
    suspend fun decodeAndDetect(uri: Uri): DetectionResult
    suspend fun cropAndFilter(uri: Uri, corners: CornerPoints, filterType: FilterType): Bitmap
}

/**
 * Android scanner engine boundary isolating OpenCV Mat direct manipulation from Compose code.
 */
class AndroidScannerEngine(private val context: Context) : ScannerEngine {

    private val scanner: DocumentScanner by lazy { DocumentScanner() }
    private val enhancer: ImageEnhancer by lazy { ImageEnhancer() }
    private var isNativeInitialized = false

    /**
     * Ensures OpenCV native runtime components are initialized.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isNativeInitialized) return@withContext true
        val openCvSuccess = try {
            System.loadLibrary("opencv_java4")
            true
        } catch (_: Throwable) {
            false
        }
        isNativeInitialized = openCvSuccess
        openCvSuccess
    }

    /**
     * Decodes source URI, applies EXIF orientation correction, and detects corners.
     */
    override suspend fun decodeAndDetect(uri: Uri): DetectionResult = withContext(Dispatchers.IO) {
        require(initialize()) { "Failed to initialize OpenCV native libraries" }

        val decodedBitmap = decodeUriWithExif(uri)
            ?: throw IllegalArgumentException("Could not decode image from provided URI")

        val sourceMat = bitmapToMat(decodedBitmap)

        val dimensions = ImageDimensions(sourceMat.cols(), sourceMat.rows())

        // Run ML/classical document corner detection
        val rawCorners = scanner.detectCorners(sourceMat)
        val (corners, autoDetected) = if (rawCorners != null && rawCorners.size == 4) {
            CornerPoints.fromArray(rawCorners) to true
        } else {
            CornerPoints.defaultInset(dimensions.width, dimensions.height, 0.10) to false
        }

        // Generate downsampled preview bitmap for fast UI rendering
        val previewBitmap = createDisplayPreview(decodedBitmap, maxDimension = 1200)

        sourceMat.release()
        decodedBitmap.recycle()

        DetectionResult(
            sourceDimensions = dimensions,
            corners = corners,
            isAutoDetected = autoDetected,
            previewBitmap = previewBitmap
        )
    }

    /**
     * Crops image using user-confirmed corners and applies requested filter.
     */
    override suspend fun cropAndFilter(
        uri: Uri,
        corners: CornerPoints,
        filterType: FilterType
    ): Bitmap = withContext(Dispatchers.IO) {
        val sourceBitmap = decodeUriWithExif(uri)
            ?: throw IllegalArgumentException("Could not decode image from provided URI")

        val sourceMat = bitmapToMat(sourceBitmap)
        sourceBitmap.recycle()

        val croppedMat = scanner.cropWarped(sourceMat, corners.toArray())
        sourceMat.release()

        val finalMat = when (filterType) {
            FilterType.ORIGINAL -> croppedMat
            FilterType.MAGIC -> {
                val enhanced = enhancer.applyMagicFilter(croppedMat)
                croppedMat.release()
                enhanced
            }
        }

        val resultBitmap = matToBitmap(finalMat)
        finalMat.release()

        resultBitmap
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val matRgba = Mat(bmp32.height, bmp32.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bmp32, matRgba)
        bmp32.recycle()

        val matBgr = Mat()
        Imgproc.cvtColor(matRgba, matBgr, Imgproc.COLOR_RGBA2BGR)
        matRgba.release()
        return matBgr
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val rgbaMat = Mat()
        if (mat.channels() == 1) {
            Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_GRAY2RGBA)
        } else if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_BGR2RGBA)
        } else {
            mat.copyTo(rgbaMat)
        }

        val bitmap = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbaMat, bitmap)
        rgbaMat.release()
        return bitmap
    }

    private fun decodeUriWithExif(uri: Uri): Bitmap? {
        val contentResolver = context.contentResolver

        val rotationDegrees = contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0

        val originalBitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        if (rotationDegrees == 0) return originalBitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(
            originalBitmap,
            0, 0,
            originalBitmap.width, originalBitmap.height,
            matrix, true
        )
        originalBitmap.recycle()
        return rotated
    }

    private fun createDisplayPreview(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val targetWidth = (width * scale).toInt()
        val targetHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}

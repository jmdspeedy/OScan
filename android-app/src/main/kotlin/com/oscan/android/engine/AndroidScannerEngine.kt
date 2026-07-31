package com.oscan.android.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.oscan.core.DocumentScanner
import com.oscan.core.ImageEnhancer
import com.oscan.core.IdCardProcessor
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    suspend fun decodeForIdCard(uri: Uri): DetectionResult = decodeAndDetect(uri)
    suspend fun cropAndFilter(uri: Uri, corners: CornerPoints, filterType: FilterType): Bitmap
    suspend fun cropIdCard(uri: Uri, corners: CornerPoints): Bitmap =
        cropAndFilter(uri, corners, FilterType.ORIGINAL)
    suspend fun createIdCardSheet(front: Bitmap, back: Bitmap): Bitmap =
        error("ID-card sheet generation is not implemented")
}

/**
 * Android scanner engine boundary isolating OpenCV Mat direct manipulation from Compose code.
 */
class AndroidScannerEngine(private val context: Context) : ScannerEngine {

    private val scanner: DocumentScanner by lazy { DocumentScanner() }
    private val enhancer: ImageEnhancer by lazy { ImageEnhancer() }
    private val idCardProcessor: IdCardProcessor by lazy { IdCardProcessor(scanner) }
    private val detectorMutex = Mutex()
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

        val decoded = decodeUriForDetection(uri)
            ?: throw IllegalArgumentException("Could not decode image from provided URI")

        val sourceMat = bitmapToMat(decoded.bitmap)
        val dimensions = decoded.sourceDimensions

        // Run ML/classical document corner detection
        val rawCorners = detectorMutex.withLock { scanner.detectCorners(sourceMat) }
        val (corners, autoDetected) = if (rawCorners != null && rawCorners.size == 4) {
            CornerPoints.fromArray(
                rawCorners.map { point ->
                    Point(point.x * decoded.scaleX, point.y * decoded.scaleY)
                }.toTypedArray()
            ) to true
        } else {
            CornerPoints.defaultInset(dimensions.width, dimensions.height, 0.10) to false
        }

        // Generate downsampled preview bitmap for fast UI rendering
        val previewBitmap = createDisplayPreview(decoded.bitmap, maxDimension = DETECTION_MAX_DIMENSION)

        sourceMat.release()
        decoded.bitmap.recycle()

        DetectionResult(
            sourceDimensions = dimensions,
            corners = corners,
            isAutoDetected = autoDetected,
            previewBitmap = previewBitmap
        )
    }

    /**
     * Detects the card boundary from the captured pixels. The on-screen guide is only a framing
     * aid; it is used as a fallback crop when neither the learned nor classical edge detector can
     * find four reliable sides.
     */
    override suspend fun decodeForIdCard(uri: Uri): DetectionResult = withContext(Dispatchers.IO) {
        require(initialize()) { "Failed to initialize OpenCV native libraries" }
        val decoded = decodeUriForDetection(uri)
            ?: throw IllegalArgumentException("Could not decode image from provided URI")
        try {
            val dimensions = decoded.sourceDimensions
            val sourceMat = bitmapToMat(decoded.bitmap)
            val detected = try {
                detectorMutex.withLock { scanner.detectCorners(sourceMat) }
            } finally {
                sourceMat.release()
            }
            val scaledDetected = detected?.map { point ->
                Point(point.x * decoded.scaleX, point.y * decoded.scaleY)
            }?.toTypedArray()
            val (corners, autoDetected) = idCardCornersOrGuide(scaledDetected, dimensions)
            DetectionResult(
                sourceDimensions = dimensions,
                corners = corners,
                isAutoDetected = autoDetected,
                previewBitmap = createDisplayPreview(decoded.bitmap, maxDimension = DETECTION_MAX_DIMENSION)
            )
        } finally {
            decoded.bitmap.recycle()
        }
    }

    /** Detects a boundary in an already oriented, downsampled camera frame. */
    suspend fun detectCameraFrame(bitmap: Bitmap): CornerPoints? = withContext(Dispatchers.Default) {
        require(initialize()) { "Failed to initialize image processing" }
        val source = bitmapToMat(bitmap)
        try {
            detectorMutex.withLock {
                scanner.detectCornersFast(source)?.takeIf { it.size == 4 }?.let(CornerPoints::fromArray)
            }
        } finally {
            source.release()
        }
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

        val finalMat = if (filterType == FilterType.ORIGINAL) {
            croppedMat
        } else {
            val enhanced = enhancer.applyFilter(croppedMat, filterType)
            croppedMat.release()
            enhanced
        }

        val resultBitmap = matToBitmap(finalMat)
        finalMat.release()

        resultBitmap
    }

    override suspend fun cropIdCard(uri: Uri, corners: CornerPoints): Bitmap = withContext(Dispatchers.IO) {
        val sourceBitmap = decodeUriWithExif(uri)
            ?: throw IllegalArgumentException("Could not decode image from provided URI")
        val sourceMat = bitmapToMat(sourceBitmap)
        sourceBitmap.recycle()
        try {
            val cropped = idCardProcessor.cropRectangle(sourceMat, corners.toArray())
            try {
                matToBitmap(cropped)
            } finally {
                cropped.release()
            }
        } finally {
            sourceMat.release()
        }
    }

    override suspend fun createIdCardSheet(front: Bitmap, back: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val frontMat = bitmapToMat(front)
        val backMat = bitmapToMat(back)
        try {
            val sheet = idCardProcessor.createSheet(frontMat, backMat)
            try {
                matToBitmap(sheet)
            } finally {
                sheet.release()
            }
        } finally {
            frontMat.release()
            backMat.release()
        }
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
        val rotationDegrees = readExifRotation(uri)

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

    /**
     * Decodes only enough pixels for edge detection and the crop preview. Camera photos can be
     * tens of megapixels, while both DocQuadNet and the classical detector operate on a much
     * smaller working image. Avoiding a full-resolution Bitmap and Mat here substantially reduces
     * the pause between the shutter and edge adjustment without changing saved image quality.
     */
    private fun decodeUriForDetection(uri: Uri): DetectionDecode? {
        val contentResolver = context.contentResolver
        val rotationDegrees = readExifRotation(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = detectionSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxDimension = DETECTION_MAX_DIMENSION
        )
        val sampled = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            )
        } ?: return null

        val oriented = if (rotationDegrees == 0) {
            sampled
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
                .also { if (it !== sampled) sampled.recycle() }
        }
        val sourceWidth = if (rotationDegrees % 180 == 0) bounds.outWidth else bounds.outHeight
        val sourceHeight = if (rotationDegrees % 180 == 0) bounds.outHeight else bounds.outWidth
        return DetectionDecode(
            bitmap = oriented,
            sourceDimensions = ImageDimensions(sourceWidth, sourceHeight),
            scaleX = sourceWidth.toDouble() / oriented.width.toDouble(),
            scaleY = sourceHeight.toDouble() / oriented.height.toDouble()
        )
    }

    private fun readExifRotation(uri: Uri): Int = context.contentResolver.openInputStream(uri)?.use { stream ->
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } ?: 0

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

    private data class DetectionDecode(
        val bitmap: Bitmap,
        val sourceDimensions: ImageDimensions,
        val scaleX: Double,
        val scaleY: Double
    )

    private companion object {
        const val DETECTION_MAX_DIMENSION = 1200
    }
}

internal fun detectionSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    require(width > 0 && height > 0 && maxDimension > 0)
    var sampleSize = 1
    while (max(width / sampleSize, height / sampleSize) > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

internal fun idCardGuideCorners(dimensions: ImageDimensions): CornerPoints {
    val width = dimensions.width.toDouble()
    val height = dimensions.height.toDouble()
    val guideWidth = width * 0.86
    val guideHeight = (guideWidth / 1.586).coerceAtMost(height * 0.72)
    val left = (width - guideWidth) / 2.0
    val top = (height - guideHeight) / 2.0
    return CornerPoints(
        Point(left, top),
        Point(left + guideWidth, top),
        Point(left + guideWidth, top + guideHeight),
        Point(left, top + guideHeight)
    )
}

internal fun idCardCornersOrGuide(
    detected: Array<Point>?,
    dimensions: ImageDimensions
): Pair<CornerPoints, Boolean> = if (detected?.size == 4) {
    CornerPoints.fromArray(detected) to true
} else {
    idCardGuideCorners(dimensions) to false
}

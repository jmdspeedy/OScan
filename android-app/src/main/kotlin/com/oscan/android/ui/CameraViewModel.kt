package com.oscan.android.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.oscan.android.R
import com.oscan.android.engine.AndroidScannerEngine
import java.io.File
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Point
import kotlin.math.abs
import kotlin.math.hypot

data class PreviewPoint(val x: Float, val y: Float)

data class CameraUiState(
    val isStarting: Boolean = true,
    val isAvailable: Boolean = true,
    val isCapturing: Boolean = false,
    val torchAvailable: Boolean = false,
    val torchEnabled: Boolean = false,
    val corners: List<PreviewPoint>? = null,
    val guidance: String = "",
    val errorMessage: String? = null
)

class CameraViewModel(
    application: Application,
    private val scannerEngine: AndroidScannerEngine
) : AndroidViewModel(application) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisBusy = AtomicBoolean(false)
    private val _uiState = MutableStateFlow(CameraUiState(guidance = application.getString(R.string.camera_point_document)))
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var lastAnalysisAt = 0L
    private var smoothedCorners: List<PreviewPoint>? = null
    private var bindingGeneration = 0
    private var boundPreviewView: WeakReference<PreviewView>? = null
    @Volatile private var previewAspect = 3f / 4f

    fun bind(owner: LifecycleOwner, previewView: PreviewView, rotation: Int = Surface.ROTATION_0) {
        val generation = ++bindingGeneration
        val future = ProcessCameraProvider.getInstance(getApplication())
        future.addListener({
            if (generation != bindingGeneration) return@addListener
            runCatching {
                val cameraProvider = future.get()
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                if (!cameraProvider.hasCamera(selector)) {
                    _uiState.value = CameraUiState(isStarting = false, isAvailable = false, errorMessage = getApplication<Application>().getString(R.string.camera_no_rear))
                    return@addListener
                }
                val preview = Preview.Builder().setTargetRotation(rotation).build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(rotation)
                    .build()
                val analysis = ImageAnalysis.Builder()
                    // The corner model downsamples to 256x256 internally. Keeping the CameraX
                    // buffer modest avoids copying/cropping unnecessary pixels on every frame.
                    .setTargetResolution(Size(640, 480))
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, ::analyze) }
                cameraProvider.unbindAll()
                boundPreviewView = WeakReference(previewView)
                val useCases = UseCaseGroup.Builder().apply {
                    previewView.viewPort?.let(::setViewPort)
                    addUseCase(preview)
                    addUseCase(capture)
                    addUseCase(analysis)
                }.build()
                camera = cameraProvider.bindToLifecycle(owner, selector, useCases)
                provider = cameraProvider
                imageCapture = capture
                val torchAvailable = camera?.cameraInfo?.hasFlashUnit() == true
                _uiState.value = _uiState.value.copy(
                    isStarting = false,
                    isAvailable = true,
                    torchAvailable = torchAvailable,
                    errorMessage = null
                )
            }.onFailure {
                _uiState.value = CameraUiState(isStarting = false, isAvailable = false, errorMessage = getApplication<Application>().getString(R.string.camera_start_failed))
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun unbind() {
        bindingGeneration++
        provider?.unbindAll()
        provider = null
        camera = null
        imageCapture = null
        boundPreviewView?.clear()
        boundPreviewView = null
    }

    fun updatePreviewSize(width: Int, height: Int) {
        if (width > 0 && height > 0) previewAspect = width.toFloat() / height
    }

    fun toggleTorch() {
        val current = _uiState.value
        if (!current.torchAvailable) return
        val enable = !current.torchEnabled
        camera?.cameraControl?.enableTorch(enable)
        _uiState.value = current.copy(torchEnabled = enable)
    }

    fun capture(destination: File, onResult: (File?) -> Unit) {
        val capture = imageCapture ?: return onResult(null)
        if (_uiState.value.isCapturing) return
        _uiState.value = _uiState.value.copy(isCapturing = true, errorMessage = null)
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(destination).build(),
            ContextCompat.getMainExecutor(getApplication()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _uiState.value = _uiState.value.copy(isCapturing = false)
                    onResult(destination)
                }

                override fun onError(exception: ImageCaptureException) {
                    destination.delete()
                    _uiState.value = _uiState.value.copy(isCapturing = false, errorMessage = getApplication<Application>().getString(R.string.camera_capture_failed))
                    onResult(null)
                }
            }
        )
    }

    private fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalysisAt < LIVE_ANALYSIS_INTERVAL_MS || !analysisBusy.compareAndSet(false, true)) {
            image.close()
            return
        }
        lastAnalysisAt = now
        val sourceTransform = runCatching {
            ImageProxyTransformFactory().apply {
                isUsingRotationDegrees = true
                isUsingCropRect = false
            }.getOutputTransform(image)
        }.getOrNull()
        val bitmap = runCatching { image.toOrientedBitmap() }.getOrNull()
        image.close()
        if (bitmap == null) {
            analysisBusy.set(false)
            return
        }
        viewModelScope.launch {
            try {
                val visible = withContext(Dispatchers.Default) { cropToAspect(bitmap, previewAspect) }
                val quality = withContext(Dispatchers.Default) { measureQuality(visible.bitmap) }
                val detected = scannerEngine.detectCameraFrame(visible.bitmap)
                val fullFramePoints = detected?.toArray()?.map {
                    Point(it.x + visible.offsetX, it.y + visible.offsetY)
                }
                val mapped = if (fullFramePoints != null && sourceTransform != null) {
                    mapToPreview(fullFramePoints, sourceTransform)
                } else null
                if (visible.bitmap !== bitmap) visible.bitmap.recycle()
                val motion = mapped != null && smoothedCorners != null && averageDistance(mapped, smoothedCorners!!) > 48f
                smoothedCorners = when {
                    mapped == null -> null
                    smoothedCorners == null || motion -> mapped
                    else -> mapped.zip(smoothedCorners!!).map { (fresh, old) ->
                        PreviewPoint(old.x * .2f + fresh.x * .8f, old.y * .2f + fresh.y * .8f)
                    }
                }
                val preview = boundPreviewView?.get()
                val nearEdge = mapped?.any {
                    it.x < 28f || it.y < 28f || it.x > (preview?.width ?: Int.MAX_VALUE) - 28f || it.y > (preview?.height ?: Int.MAX_VALUE) - 28f
                } == true
                val guidance = when {
                    quality.brightness < 42 -> getApplication<Application>().getString(R.string.camera_more_light)
                    quality.sharpness < 7 -> getApplication<Application>().getString(R.string.camera_hold_sharp)
                    mapped == null -> getApplication<Application>().getString(R.string.camera_whole_document)
                    nearEdge -> getApplication<Application>().getString(R.string.camera_move_back)
                    motion -> getApplication<Application>().getString(R.string.camera_hold_steady)
                    else -> getApplication<Application>().getString(R.string.camera_document_found)
                }
                _uiState.value = _uiState.value.copy(
                    corners = smoothedCorners,
                    guidance = guidance
                )
            } catch (_: Throwable) {
                _uiState.value = _uiState.value.copy(corners = null, guidance = getApplication<Application>().getString(R.string.camera_point_document))
            } finally {
                bitmap.recycle()
                analysisBusy.set(false)
            }
        }
    }

    private suspend fun mapToPreview(points: List<Point>, source: OutputTransform): List<PreviewPoint>? =
        withContext(Dispatchers.Main.immediate) {
            val target = boundPreviewView?.get()?.outputTransform ?: return@withContext null
            val values = FloatArray(points.size * 2)
            points.forEachIndexed { index, point ->
                values[index * 2] = point.x.toFloat()
                values[index * 2 + 1] = point.y.toFloat()
            }
            CoordinateTransform(source, target).mapPoints(values)
            values.toList().chunked(2).map { PreviewPoint(it[0], it[1]) }
        }

    override fun onCleared() {
        unbind()
        analysisExecutor.shutdown()
    }

    private companion object {
        /** 15 Hz target; slower devices naturally fall back to one update per completed inference. */
        const val LIVE_ANALYSIS_INTERVAL_MS = 67L
    }
}

private data class FrameQuality(val brightness: Int, val sharpness: Int)

private fun measureQuality(bitmap: Bitmap): FrameQuality {
    val step = 8
    var total = 0L
    var gradients = 0L
    var samples = 0
    var previous = -1
    for (y in 0 until bitmap.height step step) {
        for (x in 0 until bitmap.width step step) {
            val pixel = bitmap.getPixel(x, y)
            val luma = ((pixel shr 16 and 0xff) * 3 + (pixel shr 8 and 0xff) * 6 + (pixel and 0xff)) / 10
            total += luma
            if (previous >= 0) gradients += abs(luma - previous)
            previous = luma
            samples++
        }
    }
    return FrameQuality((total / samples.coerceAtLeast(1)).toInt(), (gradients / samples.coerceAtLeast(1)).toInt())
}

private fun averageDistance(a: List<PreviewPoint>, b: List<PreviewPoint>): Float =
    a.zip(b).map { (p, q) -> hypot(p.x - q.x, p.y - q.y) }.average().toFloat()

private data class VisibleFrame(val bitmap: Bitmap, val offsetX: Int, val offsetY: Int)

private fun cropToAspect(bitmap: Bitmap, targetAspect: Float): VisibleFrame {
    val sourceAspect = bitmap.width.toFloat() / bitmap.height
    if (kotlin.math.abs(sourceAspect - targetAspect) < .01f) return VisibleFrame(bitmap, 0, 0)
    return if (sourceAspect > targetAspect) {
        val width = (bitmap.height * targetAspect).toInt().coerceIn(1, bitmap.width)
        val x = (bitmap.width - width) / 2
        VisibleFrame(Bitmap.createBitmap(bitmap, x, 0, width, bitmap.height), x, 0)
    } else {
        val height = (bitmap.width / targetAspect).toInt().coerceIn(1, bitmap.height)
        val y = (bitmap.height - height) / 2
        VisibleFrame(Bitmap.createBitmap(bitmap, 0, y, bitmap.width, height), 0, y)
    }
}

private fun ImageProxy.toOrientedBitmap(): Bitmap {
    val plane = planes.first()
    val buffer: ByteBuffer = plane.buffer
    buffer.rewind()
    val rowPadding = plane.rowStride - plane.pixelStride * width
    val padded = Bitmap.createBitmap(width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
    padded.copyPixelsFromBuffer(buffer)
    val cropped = if (padded.width == width) padded else Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
    val degrees = imageInfo.rotationDegrees
    if (degrees == 0) return cropped
    val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    cropped.recycle()
    return rotated
}

package com.oscan.android.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Page
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.engine.ScannerEngine
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import com.oscan.core.util.CoordinateTransformer
import com.oscan.core.util.CornerValidator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Point

sealed interface PageEditorUiState {
    data object Loading : PageEditorUiState
    data class CropReady(
        val previewBitmap: Bitmap,
        val corners: CornerPoints,
        val initialDetectedCorners: CornerPoints,
        val isValidGeometry: Boolean
    ) : PageEditorUiState
    data class Processing(val message: String) : PageEditorUiState
    data class PreviewReady(
        val selectedFilter: FilterType,
        val croppedBitmap: Bitmap
    ) : PageEditorUiState
    data object Saved : PageEditorUiState
    data class Error(val message: String, val previousState: PageEditorUiState?) : PageEditorUiState
}

class PageEditorViewModel(
    private val documentId: DocumentId,
    val page: Page,
    private val fileStore: DocumentFileStore,
    private val scannerEngine: ScannerEngine,
    private val repository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PageEditorUiState>(PageEditorUiState.Loading)
    val uiState: StateFlow<PageEditorUiState> = _uiState.asStateFlow()

    private var sourceUri: Uri? = null
    private var sourceWidth: Int = page.width
    private var sourceHeight: Int = page.height
    private val treatmentCache = mutableMapOf<FilterType, Bitmap>()
    private var currentCorners: CornerPoints? = null

    init {
        loadSourceImage()
    }

    private fun loadSourceImage() {
        viewModelScope.launch {
            _uiState.value = PageEditorUiState.Loading
            runCatching {
                val file = fileStore.resolve(page.originalAsset)
                require(file.exists() && file.isFile) { "Source asset not found" }
                val uri = Uri.fromFile(file)
                sourceUri = uri
                val result = scannerEngine.decodeAndDetect(uri)
                sourceWidth = result.sourceDimensions.width
                sourceHeight = result.sourceDimensions.height
                result
            }.onSuccess { result ->
                currentCorners = result.corners
                _uiState.value = PageEditorUiState.CropReady(
                    previewBitmap = result.previewBitmap,
                    corners = result.corners,
                    initialDetectedCorners = result.corners,
                    isValidGeometry = CornerValidator.isValidQuadrilateral(
                        result.corners.toArray(),
                        result.sourceDimensions
                    )
                )
            }.onFailure {
                _uiState.value = PageEditorUiState.Error("Source image could not be loaded.", null)
            }
        }
    }

    fun onCornerMoved(handleIndex: Int, newDisplayPoint: Point, containerDimensions: ImageDimensions) {
        val current = _uiState.value as? PageEditorUiState.CropReady ?: return
        if (handleIndex !in 0..3) return
        val sourceDimensions = ImageDimensions(sourceWidth, sourceHeight)
        val transform = CoordinateTransformer.computeTransform(sourceDimensions, containerDimensions)
        val points = current.corners.toArray()
        points[handleIndex] = CoordinateTransformer.displayToSource(newDisplayPoint, transform, sourceDimensions)
        val corners = CornerPoints.fromArray(points)
        _uiState.value = current.copy(
            corners = corners,
            isValidGeometry = CornerValidator.isValidQuadrilateral(points, sourceDimensions)
        )
    }

    fun onResetCorners() {
        val current = _uiState.value as? PageEditorUiState.CropReady ?: return
        val dimensions = ImageDimensions(sourceWidth, sourceHeight)
        _uiState.value = current.copy(
            corners = current.initialDetectedCorners,
            isValidGeometry = CornerValidator.isValidQuadrilateral(current.initialDetectedCorners.toArray(), dimensions)
        )
    }

    fun onCropConfirmed() {
        val current = _uiState.value as? PageEditorUiState.CropReady ?: return
        if (!current.isValidGeometry) return
        val uri = sourceUri ?: return
        currentCorners = current.corners
        treatmentCache.values.forEach { if (!it.isRecycled) it.recycle() }
        treatmentCache.clear()

        viewModelScope.launch {
            _uiState.value = PageEditorUiState.Processing("Straightening page…")
            runCatching {
                scannerEngine.cropAndFilter(uri, current.corners, FilterType.ORIGINAL)
            }.onSuccess { bitmap ->
                treatmentCache[FilterType.ORIGINAL] = bitmap
                _uiState.value = PageEditorUiState.PreviewReady(FilterType.ORIGINAL, bitmap)
            }.onFailure {
                _uiState.value = PageEditorUiState.Error("Could not crop page with selected edges.", current)
            }
        }
    }

    fun onFilterSelected(filter: FilterType) {
        val current = _uiState.value as? PageEditorUiState.PreviewReady ?: return
        if (current.selectedFilter == filter) return

        val cached = treatmentCache[filter]
        if (cached != null && !cached.isRecycled) {
            _uiState.value = current.copy(selectedFilter = filter, croppedBitmap = cached)
            return
        }

        val uri = sourceUri ?: return
        val corners = currentCorners ?: return
        viewModelScope.launch {
            _uiState.value = PageEditorUiState.Processing("Applying treatment…")
            runCatching {
                scannerEngine.cropAndFilter(uri, corners, filter)
            }.onSuccess { bitmap ->
                treatmentCache[filter] = bitmap
                _uiState.value = PageEditorUiState.PreviewReady(filter, bitmap)
            }.onFailure {
                _uiState.value = PageEditorUiState.Error("Treatment could not be applied.", current)
            }
        }
    }

    fun onBackToCrop() {
        loadSourceImage()
    }

    fun saveEdits() {
        val current = _uiState.value as? PageEditorUiState.PreviewReady ?: return
        val bitmap = current.croppedBitmap
        viewModelScope.launch {
            _uiState.value = PageEditorUiState.Processing("Saving page updates…")
            runCatching {
                withContext(Dispatchers.IO) {
                    val processedBytes = ByteArrayOutputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.toByteArray()
                    }
                    val thumbnailBitmap = createThumbnail(bitmap)
                    val thumbnailBytes = try {
                        ByteArrayOutputStream().use { out ->
                            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            out.toByteArray()
                        }
                    } finally {
                        if (thumbnailBitmap !== bitmap) thumbnailBitmap.recycle()
                    }
                    repository.updatePageAssets(
                        id = documentId,
                        pageId = page.id,
                        processedStream = { ByteArrayInputStream(processedBytes) },
                        thumbnailStream = { ByteArrayInputStream(thumbnailBytes) },
                        width = bitmap.width,
                        height = bitmap.height
                    )
                }
            }.onSuccess {
                _uiState.value = PageEditorUiState.Saved
            }.onFailure {
                _uiState.value = PageEditorUiState.Error("Changes could not be saved.", current)
            }
        }
    }

    fun dismissError() {
        val current = _uiState.value as? PageEditorUiState.Error ?: return
        _uiState.value = current.previousState ?: PageEditorUiState.Loading
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val maxSize = 360
        if (bitmap.width <= maxSize && bitmap.height <= maxSize) return bitmap
        val scale = min(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    override fun onCleared() {
        super.onCleared()
        treatmentCache.values.forEach { if (!it.isRecycled) it.recycle() }
        treatmentCache.clear()
    }
}

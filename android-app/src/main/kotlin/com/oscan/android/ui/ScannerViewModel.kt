package com.oscan.android.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscan.android.engine.AndroidPdfExporter
import com.oscan.android.engine.AndroidScannerEngine
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import com.oscan.core.util.CoordinateTransformer
import com.oscan.core.util.CornerValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.core.Point
import java.io.File

sealed interface ScannerUiState {
    object Empty : ScannerUiState

    data class Detecting(val uri: Uri) : ScannerUiState

    data class CropReady(
        val uri: Uri,
        val previewBitmap: Bitmap,
        val sourceDimensions: ImageDimensions,
        val corners: CornerPoints,
        val initialDetectedCorners: CornerPoints,
        val isAutoDetected: Boolean,
        val isValidGeometry: Boolean
    ) : ScannerUiState

    data class ProcessingCrop(val uri: Uri) : ScannerUiState

    data class PreviewReady(
        val uri: Uri,
        val sourceDimensions: ImageDimensions,
        val corners: CornerPoints,
        val selectedFilter: FilterType,
        val croppedBitmap: Bitmap
    ) : ScannerUiState

    object Exporting : ScannerUiState

    data class ExportSuccess(val pdfUri: Uri) : ScannerUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val previousState: ScannerUiState? = null
    ) : ScannerUiState
}

class ScannerViewModel(
    private val scannerEngine: AndroidScannerEngine,
    private val pdfExporter: AndroidPdfExporter = AndroidPdfExporter()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Empty)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Detecting(uri)
            try {
                val detectionResult = scannerEngine.decodeAndDetect(uri)
                val isValid = CornerValidator.isValidQuadrilateral(
                    corners = detectionResult.corners.toArray(),
                    dimensions = detectionResult.sourceDimensions
                )
                _uiState.value = ScannerUiState.CropReady(
                    uri = uri,
                    previewBitmap = detectionResult.previewBitmap,
                    sourceDimensions = detectionResult.sourceDimensions,
                    corners = detectionResult.corners,
                    initialDetectedCorners = detectionResult.corners,
                    isAutoDetected = detectionResult.isAutoDetected,
                    isValidGeometry = isValid
                )
            } catch (e: Throwable) {
                _uiState.value = ScannerUiState.Error(
                    message = e.localizedMessage ?: "Failed to process selected image",
                    canRetry = true
                )
            }
        }
    }

    fun onCornerMoved(
        handleIndex: Int,
        newDisplayPoint: Point,
        containerDimensions: ImageDimensions
    ) {
        val currentState = _uiState.value as? ScannerUiState.CropReady ?: return

        val transform = CoordinateTransformer.computeTransform(
            source = currentState.sourceDimensions,
            container = containerDimensions
        )
        val newSourcePoint = CoordinateTransformer.displayToSource(
            displayPoint = newDisplayPoint,
            transform = transform,
            source = currentState.sourceDimensions
        )

        val cornersArray = currentState.corners.toArray()
        cornersArray[handleIndex] = newSourcePoint
        val updatedCorners = CornerPoints.fromArray(cornersArray)

        val isValid = CornerValidator.isValidQuadrilateral(
            corners = cornersArray,
            dimensions = currentState.sourceDimensions
        )

        _uiState.value = currentState.copy(
            corners = updatedCorners,
            isValidGeometry = isValid
        )
    }

    fun onResetCorners() {
        val currentState = _uiState.value as? ScannerUiState.CropReady ?: return
        val resetCorners = currentState.initialDetectedCorners
        val isValid = CornerValidator.isValidQuadrilateral(
            corners = resetCorners.toArray(),
            dimensions = currentState.sourceDimensions
        )
        _uiState.value = currentState.copy(
            corners = resetCorners,
            isValidGeometry = isValid
        )
    }

    fun onCropConfirmed() {
        val currentState = _uiState.value as? ScannerUiState.CropReady ?: return
        if (!currentState.isValidGeometry) return

        viewModelScope.launch {
            _uiState.value = ScannerUiState.ProcessingCrop(currentState.uri)
            try {
                val croppedBitmap = scannerEngine.cropAndFilter(
                    uri = currentState.uri,
                    corners = currentState.corners,
                    filterType = FilterType.ORIGINAL
                )
                _uiState.value = ScannerUiState.PreviewReady(
                    uri = currentState.uri,
                    sourceDimensions = currentState.sourceDimensions,
                    corners = currentState.corners,
                    selectedFilter = FilterType.ORIGINAL,
                    croppedBitmap = croppedBitmap
                )
            } catch (e: Throwable) {
                _uiState.value = ScannerUiState.Error(
                    message = e.localizedMessage ?: "Crop operation failed",
                    canRetry = true,
                    previousState = currentState
                )
            }
        }
    }

    fun onFilterSelected(filterType: FilterType) {
        val currentState = _uiState.value as? ScannerUiState.PreviewReady ?: return
        if (currentState.selectedFilter == filterType) return

        viewModelScope.launch {
            _uiState.value = ScannerUiState.ProcessingCrop(currentState.uri)
            try {
                val filteredBitmap = scannerEngine.cropAndFilter(
                    uri = currentState.uri,
                    corners = currentState.corners,
                    filterType = filterType
                )
                _uiState.value = currentState.copy(
                    selectedFilter = filterType,
                    croppedBitmap = filteredBitmap
                )
            } catch (e: Throwable) {
                _uiState.value = ScannerUiState.Error(
                    message = e.localizedMessage ?: "Failed to apply filter",
                    canRetry = true,
                    previousState = currentState
                )
            }
        }
    }

    fun onExportPdfDestinationSelected(context: Context, destinationUri: Uri) {
        val currentState = _uiState.value as? ScannerUiState.PreviewReady ?: return

        viewModelScope.launch {
            val previousState = currentState
            _uiState.value = ScannerUiState.Exporting
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { stream ->
                    pdfExporter.exportToPdf(currentState.croppedBitmap, stream)
                } ?: throw IllegalStateException("Could not open destination output stream")

                _uiState.value = ScannerUiState.ExportSuccess(destinationUri)
            } catch (e: Throwable) {
                _uiState.value = ScannerUiState.Error(
                    message = e.localizedMessage ?: "Failed to export PDF",
                    canRetry = true,
                    previousState = previousState
                )
            }
        }
    }

    fun sharePdf(context: Context, pdfUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document PDF"))
    }

    fun onBackToCrop() {
        val currentState = _uiState.value as? ScannerUiState.PreviewReady ?: return
        onImageSelected(currentState.uri)
    }

    fun onResetToEmpty() {
        _uiState.value = ScannerUiState.Empty
    }

    fun dismissError() {
        val currentError = _uiState.value as? ScannerUiState.Error
        _uiState.value = currentError?.previousState ?: ScannerUiState.Empty
    }
}

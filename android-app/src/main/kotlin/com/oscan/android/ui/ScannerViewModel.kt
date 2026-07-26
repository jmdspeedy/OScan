package com.oscan.android.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.NewPage
import com.oscan.android.data.session.ScanSession
import com.oscan.android.data.session.ScanSessionStore
import com.oscan.android.data.session.SessionPage
import com.oscan.android.data.session.SessionPageStatus
import com.oscan.android.engine.ScannerEngine
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import com.oscan.core.IdCardProcessor
import com.oscan.core.util.CoordinateTransformer
import com.oscan.core.util.CornerValidator
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.opencv.core.Point

data class SessionPageSummary(
    val id: String,
    val position: Int,
    val status: SessionPageStatus,
    val thumbnail: Bitmap?,
    val message: String?,
    val canRetryDirectly: Boolean
)

sealed interface ScannerUiState {
    data object Empty : ScannerUiState
    data object LoadingSession : ScannerUiState
    data class Importing(val completed: Int, val total: Int, val session: ScanSession) : ScannerUiState
    data class CropReady(
        val session: ScanSession,
        val page: SessionPage,
        val previewBitmap: Bitmap,
        val corners: CornerPoints,
        val initialDetectedCorners: CornerPoints,
        val isValidGeometry: Boolean
    ) : ScannerUiState
    data class IdCardAdjust(
        val session: ScanSession,
        val frontPage: SessionPage,
        val backPage: SessionPage,
        val frontPreview: Bitmap,
        val backPreview: Bitmap,
        val frontCorners: CornerPoints,
        val backCorners: CornerPoints,
        val frontInitialCorners: CornerPoints,
        val backInitialCorners: CornerPoints,
        val isFrontValid: Boolean,
        val isBackValid: Boolean
    ) : ScannerUiState
    data class Processing(val session: ScanSession, val message: String) : ScannerUiState
    data class PreviewReady(
        val session: ScanSession,
        val page: SessionPage,
        val selectedFilter: FilterType,
        val croppedBitmap: Bitmap
    ) : ScannerUiState
    data class Review(val session: ScanSession, val pages: List<SessionPageSummary>) : ScannerUiState
    data class SaveDocument(
        val session: ScanSession,
        val folders: List<Folder>,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : ScannerUiState
    data class Saved(val documentId: DocumentId, val name: String, val pageCount: Int) : ScannerUiState
    data class Error(val message: String, val previousState: ScannerUiState?) : ScannerUiState
}

class ScannerViewModel(
    private val scannerEngine: ScannerEngine,
    private val repository: DocumentRepository,
    private val sessionStore: ScanSessionStore,
    private val contentResolver: ContentResolver,
    private val defaultFilterProvider: suspend () -> FilterType = { FilterType.MAGIC }
) : ViewModel() {

    private var session: ScanSession? = sessionStore.loadActive()
    private val _uiState = MutableStateFlow<ScannerUiState>(
        if (session == null) ScannerUiState.Empty else ScannerUiState.LoadingSession
    )
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    private val _cameraCaptureState = MutableStateFlow(CameraCaptureState())
    val cameraCaptureState: StateFlow<CameraCaptureState> = _cameraCaptureState.asStateFlow()
    private var activeCameraImports = 0
    private var activeCameraImportJob: Job? = null
    private val cameraDetectionMutex = Mutex()
    private var pendingIdCardAdjustment: ScannerUiState.IdCardAdjust? = null

    init {
        session?.let { restored ->
            viewModelScope.launch {
                val current = restored.currentPageId?.let { id -> restored.pages.find { it.id == id } }
                if (current != null && current.status != SessionPageStatus.FAILED) openPage(current.id)
                else showReview()
            }
        }
    }

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val draft = session ?: sessionStore.create().also { session = it }
            val startPosition = draft.pages.size
            val placeholders = uris.mapIndexed { index, _ ->
                SessionPage(
                    id = sessionStore.newPageId(),
                    position = startPosition + index,
                    status = SessionPageStatus.IMPORTING
                )
            }
            updateSession(draft.copy(pages = draft.pages + placeholders))
            uris.forEachIndexed { index, uri ->
                _uiState.value = ScannerUiState.Importing(index, uris.size, requireSession())
                importAndDetect(placeholders[index].id, uri)
            }
            _uiState.value = ScannerUiState.Importing(uris.size, uris.size, requireSession())
            val firstNewReviewable = placeholders.firstNotNullOfOrNull { placeholder ->
                requireSession().pages.find { it.id == placeholder.id && it.status == SessionPageStatus.CROP_REVIEW }?.id
            }
            if (firstNewReviewable != null) openPage(firstNewReviewable) else showReview()
        }
    }

    /** Copies a CameraX output into durable session storage before deleting the transient file. */
    fun onCameraCaptured(file: File, mode: CameraScanMode) {
        activeCameraImports++
        _cameraCaptureState.value = _cameraCaptureState.value.copy(
            isProcessing = true,
            message = null,
            mode = mode
        )
        activeCameraImportJob = viewModelScope.launch {
            val draft = session ?: sessionStore.create().also { session = it }
            val page = SessionPage(
                id = sessionStore.newPageId(),
                position = draft.pages.size,
                status = SessionPageStatus.IMPORTING
            )
            updateSession(draft.copy(pages = draft.pages + page))
            try {
                val sourcePath = withContext(Dispatchers.IO) {
                    FileInputStream(file).use { input ->
                        sessionStore.importSource(requireSession().id, page.id, input, "jpg")
                    }
                }
                updatePage(page.id) {
                    it.copy(status = SessionPageStatus.DETECTING, sourcePath = sourcePath, originalExtension = "jpg")
                }
                _cameraCaptureState.value = _cameraCaptureState.value.copy(
                    capturedCount = requireSession().pages.count { it.sourcePath != null }
                )
                cameraDetectionMutex.withLock {
                    if (mode == CameraScanMode.IdCard) prepareIdCardPage(page.id) else detectStoredPage(page.id)
                }
                val latest = requireSession().pages.first { it.id == page.id }
                _cameraCaptureState.value = _cameraCaptureState.value.copy(
                    capturedCount = requireSession().pages.count { it.sourcePath != null },
                    message = if (latest.status == SessionPageStatus.FAILED) "That photo could not be prepared. You can keep scanning." else null
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                updatePage(page.id) {
                    it.copy(status = SessionPageStatus.FAILED, failureMessage = "This photo could not be added. Remove it or try again.")
                }
                _cameraCaptureState.value = _cameraCaptureState.value.copy(
                    message = "That photo could not be added. Try again."
                )
            } finally {
                file.delete()
                activeCameraImports--
                _cameraCaptureState.value = _cameraCaptureState.value.copy(isProcessing = activeCameraImports > 0)
            }
        }
    }

    /** Drops an abandoned, incomplete front-only ID-card capture and its stored image. */
    fun discardIncompleteIdCardCapture() {
        val capture = _cameraCaptureState.value
        if (capture.mode != CameraScanMode.IdCard || (capture.capturedCount == 0 && !capture.isProcessing)) return
        if (capture.capturedCount >= 2) return
        activeCameraImportJob?.cancel()
        activeCameraImportJob = null
        discardSession()
    }

    fun finishCameraCapture() {
        val draft = session ?: return
        if (_cameraCaptureState.value.mode == CameraScanMode.IdCard && draft.pages.size >= 2) {
            showIdCardAdjustment()
            return
        }
        val first = draft.pages.sortedBy { it.position }.firstOrNull { it.status == SessionPageStatus.CROP_REVIEW }
        if (first != null) openPage(first.id) else showReview()
    }

    private fun showIdCardAdjustment() {
        viewModelScope.launch {
            val draft = requireSession()
            val pages = draft.pages.sortedBy { it.position }.take(2)
            if (pages.size != 2 || pages.any { it.sourcePath == null }) return@launch showReview()
            _uiState.value = ScannerUiState.Processing(draft, "Preparing both sides\u2026")
            runCatching {
                val frontResult = scannerEngine.decodeForIdCard(sourceUri(pages[0]))
                val backResult = scannerEngine.decodeForIdCard(sourceUri(pages[1]))
                val front = requireSession().pages.first { it.id == pages[0].id }
                val back = requireSession().pages.first { it.id == pages[1].id }
                val frontCorners = front.corners ?: frontResult.corners
                val backCorners = back.corners ?: backResult.corners
                ScannerUiState.IdCardAdjust(
                    session = requireSession(),
                    frontPage = front,
                    backPage = back,
                    frontPreview = frontResult.previewBitmap,
                    backPreview = backResult.previewBitmap,
                    frontCorners = frontCorners,
                    backCorners = backCorners,
                    frontInitialCorners = front.initialCorners ?: frontResult.corners,
                    backInitialCorners = back.initialCorners ?: backResult.corners,
                    isFrontValid = CornerValidator.isValidQuadrilateral(frontCorners.toArray(), frontResult.sourceDimensions),
                    isBackValid = CornerValidator.isValidQuadrilateral(backCorners.toArray(), backResult.sourceDimensions)
                )
            }.onSuccess {
                pendingIdCardAdjustment = it
                _uiState.value = it
            }
                .onFailure {
                    _uiState.value = ScannerUiState.Error("Both card sides could not be prepared. Try again.", ScannerUiState.Empty)
                }
        }
    }

    fun onIdCardCornerMoved(
        pageId: String,
        handleIndex: Int,
        newDisplayPoint: Point,
        containerDimensions: ImageDimensions
    ) {
        val current = _uiState.value as? ScannerUiState.IdCardAdjust ?: return
        if (handleIndex !in 0..3) return
        val page = if (pageId == current.frontPage.id) current.frontPage else current.backPage
        val sourceDimensions = ImageDimensions(page.sourceWidth, page.sourceHeight)
        val transform = CoordinateTransformer.computeTransform(sourceDimensions, containerDimensions)
        val existing = if (pageId == current.frontPage.id) current.frontCorners else current.backCorners
        val points = existing.toArray()
        points[handleIndex] = CoordinateTransformer.displayToSource(newDisplayPoint, transform, sourceDimensions)
        val corners = CornerPoints.fromArray(points)
        val valid = CornerValidator.isValidQuadrilateral(points, sourceDimensions)
        _uiState.value = if (pageId == current.frontPage.id) {
            current.copy(frontCorners = corners, isFrontValid = valid)
        } else {
            current.copy(backCorners = corners, isBackValid = valid)
        }
    }

    fun resetIdCardCorners() {
        val current = _uiState.value as? ScannerUiState.IdCardAdjust ?: return
        _uiState.value = current.copy(
            frontCorners = current.frontInitialCorners,
            backCorners = current.backInitialCorners,
            isFrontValid = true,
            isBackValid = true
        )
    }

    fun confirmIdCardAdjustment() {
        val current = _uiState.value as? ScannerUiState.IdCardAdjust ?: return
        if (!current.isFrontValid || !current.isBackValid) return
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing(requireSession(), "Creating ID card page\u2026")
            runCatching {
                val front = scannerEngine.cropIdCard(sourceUri(current.frontPage), current.frontCorners)
                val back = scannerEngine.cropIdCard(sourceUri(current.backPage), current.backCorners)
                val sheet = try {
                    scannerEngine.createIdCardSheet(front, back)
                } finally {
                    front.recycle()
                    back.recycle()
                }
                val pageId = sessionStore.newPageId()
                val sourcePath = try {
                    sessionStore.writeGeneratedSource(requireSession().id, pageId, sheet)
                } finally {
                    sheet.recycle()
                }
                val dimensions = ImageDimensions(IdCardProcessor.SHEET_WIDTH, IdCardProcessor.SHEET_HEIGHT)
                val corners = fullFrameCorners(dimensions)
                val filter = runCatching { defaultFilterProvider() }.getOrDefault(FilterType.MAGIC)
                val page = SessionPage(
                    id = pageId,
                    position = 0,
                    status = SessionPageStatus.TREATMENT_REVIEW,
                    sourcePath = sourcePath,
                    originalExtension = "jpg",
                    sourceWidth = dimensions.width,
                    sourceHeight = dimensions.height,
                    corners = corners,
                    initialCorners = corners,
                    filter = filter
                )
                updateSession(requireSession().copy(pages = listOf(page), currentPageId = page.id))
                val treated = scannerEngine.cropAndFilter(sourceUri(page), corners, filter)
                page to treated
            }.onSuccess { (page, bitmap) ->
                _uiState.value = ScannerUiState.PreviewReady(requireSession(), page, page.filter, bitmap)
            }.onFailure {
                _uiState.value = ScannerUiState.Error("The ID card page could not be created. Try again.", current)
            }
        }
    }

    fun onReplacementSelected(pageId: String, uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            updatePage(pageId) { it.copy(status = SessionPageStatus.IMPORTING, failureMessage = null) }
            _uiState.value = ScannerUiState.Importing(0, 1, requireSession())
            importAndDetect(pageId, uri)
            val page = requireSession().pages.first { it.id == pageId }
            if (page.status == SessionPageStatus.CROP_REVIEW) openPage(pageId) else showReview()
        }
    }

    fun retryPage(pageId: String) {
        viewModelScope.launch {
            val page = requireSession().pages.find { it.id == pageId } ?: return@launch
            if (page.sourcePath == null) return@launch
            detectStoredPage(pageId)
            val updated = requireSession().pages.first { it.id == pageId }
            if (updated.status == SessionPageStatus.CROP_REVIEW) openPage(pageId) else showReview()
        }
    }

    fun openPage(pageId: String) {
        viewModelScope.launch {
            val draft = requireSession()
            val page = draft.pages.find { it.id == pageId } ?: return@launch
            updateSession(draft.copy(currentPageId = pageId))
            when (page.status) {
                SessionPageStatus.ACCEPTED -> openAcceptedPage(page)
                SessionPageStatus.TREATMENT_REVIEW -> recreateTreatmentPreview(page)
                SessionPageStatus.CROP_REVIEW -> openCrop(page)
                SessionPageStatus.FAILED -> showReview()
                else -> detectStoredPage(pageId).also { openPage(pageId) }
            }
        }
    }

    fun onCornerMoved(handleIndex: Int, newDisplayPoint: Point, containerDimensions: ImageDimensions) {
        val current = _uiState.value as? ScannerUiState.CropReady ?: return
        if (handleIndex !in 0..3) return
        val sourceDimensions = ImageDimensions(current.page.sourceWidth, current.page.sourceHeight)
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
        val current = _uiState.value as? ScannerUiState.CropReady ?: return
        val dimensions = ImageDimensions(current.page.sourceWidth, current.page.sourceHeight)
        _uiState.value = current.copy(
            corners = current.initialDetectedCorners,
            isValidGeometry = CornerValidator.isValidQuadrilateral(current.initialDetectedCorners.toArray(), dimensions)
        )
    }

    fun onCropConfirmed() {
        val current = _uiState.value as? ScannerUiState.CropReady ?: return
        if (!current.isValidGeometry) return
        viewModelScope.launch {
            val defaultFilter = runCatching { defaultFilterProvider() }.getOrDefault(FilterType.MAGIC)
            updatePage(current.page.id) {
                it.copy(status = SessionPageStatus.PROCESSING, corners = current.corners, filter = defaultFilter)
            }
            _uiState.value = ScannerUiState.Processing(requireSession(), "Straightening page…")
            runCatching {
                scannerEngine.cropAndFilter(sourceUri(current.page), current.corners, defaultFilter)
            }.onSuccess { bitmap ->
                updatePage(current.page.id) {
                    it.copy(status = SessionPageStatus.TREATMENT_REVIEW, corners = current.corners, filter = defaultFilter)
                }
                val page = requireSession().pages.first { it.id == current.page.id }
                _uiState.value = ScannerUiState.PreviewReady(requireSession(), page, defaultFilter, bitmap)
            }.onFailure {
                updatePage(current.page.id) { it.copy(status = SessionPageStatus.CROP_REVIEW) }
                _uiState.value = ScannerUiState.Error("This page could not be cropped. Try adjusting its edges.", current)
            }
        }
    }

    fun onFilterSelected(filter: FilterType) {
        val current = _uiState.value as? ScannerUiState.PreviewReady ?: return
        if (current.selectedFilter == filter) return
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing(requireSession(), "Applying treatment…")
            runCatching {
                val corners = current.page.corners ?: error("Missing crop")
                scannerEngine.cropAndFilter(sourceUri(current.page), corners, filter)
            }.onSuccess { bitmap ->
                updatePage(current.page.id) { it.copy(status = SessionPageStatus.TREATMENT_REVIEW, filter = filter) }
                val page = requireSession().pages.first { it.id == current.page.id }
                _uiState.value = ScannerUiState.PreviewReady(requireSession(), page, filter, bitmap)
            }.onFailure {
                _uiState.value = ScannerUiState.Error("The treatment could not be applied. Try again.", current)
            }
        }
    }

    fun acceptCurrentPage() {
        val current = _uiState.value as? ScannerUiState.PreviewReady ?: return
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing(requireSession(), "Saving page to this scan…")
            runCatching {
                val processed = sessionStore.writeProcessed(requireSession().id, current.page.id, current.croppedBitmap)
                val thumbnailBitmap = createThumbnail(current.croppedBitmap)
                val thumbnail = try {
                    sessionStore.writeThumbnail(requireSession().id, current.page.id, thumbnailBitmap)
                } finally {
                    if (thumbnailBitmap !== current.croppedBitmap) thumbnailBitmap.recycle()
                }
                updatePage(current.page.id) {
                    it.copy(
                        status = SessionPageStatus.ACCEPTED,
                        processedPath = processed,
                        thumbnailPath = thumbnail,
                        outputWidth = current.croppedBitmap.width,
                        outputHeight = current.croppedBitmap.height,
                        filter = current.selectedFilter,
                        failureMessage = null
                    )
                }
            }.onSuccess {
                pendingIdCardAdjustment = null
                val pages = requireSession().pages.sortedBy { it.position }
                val next = pages.firstOrNull { it.position > current.page.position && it.status == SessionPageStatus.CROP_REVIEW }
                    ?: pages.firstOrNull { it.status == SessionPageStatus.CROP_REVIEW }
                if (next != null) openPage(next.id) else showReview()
            }.onFailure {
                _uiState.value = ScannerUiState.Error("This page could not be added to the scan. Try again.", current)
            }
        }
    }

    fun onBackToCrop() {
        val current = _uiState.value as? ScannerUiState.PreviewReady ?: return
        val idCardAdjustment = pendingIdCardAdjustment
        if (idCardAdjustment != null) {
            updateSession(idCardAdjustment.session)
            _uiState.value = idCardAdjustment.copy(session = requireSession())
            return
        }
        viewModelScope.launch { openCrop(current.page) }
    }

    fun showReview() {
        val draft = session ?: run { _uiState.value = ScannerUiState.Empty; return }
        updateSession(draft.copy(currentPageId = null))
        _uiState.value = ScannerUiState.Review(requireSession(), summaries(requireSession()))
    }

    fun removePage(pageId: String) {
        val draft = session ?: return
        sessionStore.deletePage(draft.id, pageId)
        val pages = draft.pages.filterNot { it.id == pageId }.mapIndexed { index, page -> page.copy(position = index) }
        if (pages.isEmpty()) {
            discardSession()
        } else {
            updateSession(draft.copy(pages = pages, currentPageId = null))
            showReview()
        }
    }

    fun movePage(pageId: String, direction: Int) {
        val draft = session ?: return
        val ordered = draft.pages.sortedBy { it.position }.toMutableList()
        val from = ordered.indexOfFirst { it.id == pageId }
        val to = (from + direction).coerceIn(0, ordered.lastIndex)
        if (from < 0 || from == to) return
        val page = ordered.removeAt(from)
        ordered.add(to, page)
        updateSession(draft.copy(pages = ordered.mapIndexed { index, item -> item.copy(position = index) }))
        showReview()
    }

    private var targetDocumentId: DocumentId? = null

    fun prepareAddPagesToDocument(documentId: DocumentId, existingName: String) {
        targetDocumentId = documentId
        val draft = sessionStore.create().copy(documentName = existingName)
        session = draft
        sessionStore.save(draft)
        _uiState.value = ScannerUiState.Empty
    }

    fun beginFinish() {
        val draft = session ?: return
        if (draft.acceptedPages.isEmpty()) return
        val target = targetDocumentId
        if (target != null) {
            saveDocument()
        } else {
            viewModelScope.launch {
                val folders = runCatching { repository.observeFolders().first() }.getOrDefault(emptyList())
                _uiState.value = ScannerUiState.SaveDocument(requireSession(), folders)
            }
        }
    }

    fun updateDocumentName(name: String) {
        val draft = session ?: return
        updateSession(draft.copy(documentName = name))
        val current = _uiState.value as? ScannerUiState.SaveDocument ?: return
        _uiState.value = current.copy(session = requireSession(), errorMessage = null)
    }

    fun selectFolder(folderId: FolderId?) {
        val draft = session ?: return
        updateSession(draft.copy(selectedFolderId = folderId?.value))
        val current = _uiState.value as? ScannerUiState.SaveDocument ?: return
        _uiState.value = current.copy(session = requireSession())
    }

    fun saveDocument() {
        val draft = requireSession()
        if (targetDocumentId == null && draft.documentName.isBlank()) {
            val current = _uiState.value as? ScannerUiState.SaveDocument
            if (current != null) _uiState.value = current.copy(errorMessage = "Enter a document name.")
            return
        }
        viewModelScope.launch {
            val saveState = _uiState.value as? ScannerUiState.SaveDocument
            if (saveState != null) _uiState.value = saveState.copy(isSaving = true, errorMessage = null)
            val accepted = draft.acceptedPages
            val newPages = accepted.map { page ->
                val source = sessionFile(page.sourcePath)
                val processed = sessionFile(page.processedPath)
                val thumbnail = sessionFile(page.thumbnailPath)
                NewPage(
                    original = source::inputStream,
                    processed = processed::inputStream,
                    thumbnail = thumbnail::inputStream,
                    originalExtension = page.originalExtension,
                    processedExtension = "jpg",
                    thumbnailExtension = "jpg",
                    width = page.outputWidth,
                    height = page.outputHeight,
                    cropCorners = page.corners
                )
            }

            val targetId = targetDocumentId
            if (targetId != null) {
                runCatching {
                    repository.addPages(targetId, newPages)
                }.onSuccess {
                    sessionStore.discard(draft.id)
                    session = null
                    targetDocumentId = null
                    _uiState.value = ScannerUiState.Saved(targetId, draft.documentName.trim(), accepted.size)
                }.onFailure { error ->
                    _uiState.value = ScannerUiState.Error(
                        if (error is com.oscan.android.data.repository.RepositoryError) error.message ?: "Could not add pages."
                        else "Pages could not be added to the document. Try again.",
                        _uiState.value
                    )
                }
            } else {
                runCatching {
                    repository.create(
                        name = draft.documentName,
                        pages = newPages,
                        folderId = draft.selectedFolderId?.let(::FolderId)
                    )
                }.onSuccess { documentId ->
                    sessionStore.discard(draft.id)
                    session = null
                    _uiState.value = ScannerUiState.Saved(documentId, draft.documentName.trim(), accepted.size)
                }.onFailure { error ->
                    val current = _uiState.value as? ScannerUiState.SaveDocument
                    if (current != null) {
                        _uiState.value = current.copy(
                            isSaving = false,
                            errorMessage = if (error is com.oscan.android.data.repository.RepositoryError) {
                                error.message
                            } else {
                                "The document could not be saved. Try again."
                            }
                        )
                    }
                }
            }
        }
    }

    fun discardSession() {
        session?.let { sessionStore.discard(it.id) }
        session = null
        pendingIdCardAdjustment = null
        activeCameraImportJob = null
        _uiState.value = ScannerUiState.Empty
        _cameraCaptureState.value = CameraCaptureState()
    }

    fun dismissError() {
        val error = _uiState.value as? ScannerUiState.Error ?: return
        _uiState.value = error.previousState ?: ScannerUiState.Empty
    }

    fun startAnother() {
        session = null
        pendingIdCardAdjustment = null
        _uiState.value = ScannerUiState.Empty
    }

    private suspend fun importAndDetect(pageId: String, uri: Uri) {
        val sourcePath = runCatching {
            val extension = when (contentResolver.getType(uri)?.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/heic" -> "heic"
                "image/heif" -> "heif"
                else -> "jpg"
            }
            contentResolver.openInputStream(uri)?.use { input ->
                sessionStore.importSource(requireSession().id, pageId, input, extension)
            } ?: error("Unavailable image")
        }.getOrElse {
            updatePage(pageId) {
                it.copy(status = SessionPageStatus.FAILED, failureMessage = "This image could not be imported. Choose it again or remove it.")
            }
            return
        }
        val extension = sourcePath.substringAfterLast('.', "jpg")
        updatePage(pageId) {
            it.copy(status = SessionPageStatus.DETECTING, sourcePath = sourcePath, originalExtension = extension, failureMessage = null)
        }
        detectStoredPage(pageId)
    }

    private suspend fun detectStoredPage(pageId: String) {
        val page = requireSession().pages.first { it.id == pageId }
        val sourcePath = page.sourcePath ?: return
        updatePage(pageId) { it.copy(status = SessionPageStatus.DETECTING, failureMessage = null) }
        runCatching { scannerEngine.decodeAndDetect(Uri.fromFile(sessionStore.resolve(sourcePath))) }
            .onSuccess { result ->
                updatePage(pageId) {
                    it.copy(
                        status = SessionPageStatus.CROP_REVIEW,
                        sourceWidth = result.sourceDimensions.width,
                        sourceHeight = result.sourceDimensions.height,
                        corners = result.corners,
                        initialCorners = result.corners,
                        isAutoDetected = result.isAutoDetected,
                        failureMessage = null
                    )
                }
                result.previewBitmap.recycle()
            }
            .onFailure {
                updatePage(pageId) {
                    it.copy(status = SessionPageStatus.FAILED, failureMessage = "This image could not be prepared. Try again or remove it.")
                }
            }
    }

    private suspend fun prepareIdCardPage(pageId: String) {
        val page = requireSession().pages.first { it.id == pageId }
        val sourcePath = page.sourcePath ?: return
        updatePage(pageId) { it.copy(status = SessionPageStatus.DETECTING, failureMessage = null) }
        runCatching { scannerEngine.decodeForIdCard(Uri.fromFile(sessionStore.resolve(sourcePath))) }
            .onSuccess { result ->
                updatePage(pageId) {
                    it.copy(
                        status = SessionPageStatus.CROP_REVIEW,
                        sourceWidth = result.sourceDimensions.width,
                        sourceHeight = result.sourceDimensions.height,
                        corners = result.corners,
                        initialCorners = result.corners,
                        isAutoDetected = result.isAutoDetected,
                        failureMessage = null
                    )
                }
                result.previewBitmap.recycle()
            }
            .onFailure {
                updatePage(pageId) {
                    it.copy(status = SessionPageStatus.FAILED, failureMessage = "This card side could not be prepared. Try again.")
                }
            }
    }

    private suspend fun openCrop(page: SessionPage) {
        val sourcePath = page.sourcePath ?: return showReview()
        _uiState.value = ScannerUiState.Processing(requireSession(), "Loading page…")
        runCatching { scannerEngine.decodeAndDetect(Uri.fromFile(sessionStore.resolve(sourcePath))) }
            .onSuccess { result ->
                val beforeUpdate = requireSession().pages.first { it.id == page.id }
                if (beforeUpdate.sourceWidth <= 0 || beforeUpdate.sourceHeight <= 0 || beforeUpdate.corners == null) {
                    updatePage(page.id) {
                        it.copy(
                            status = SessionPageStatus.CROP_REVIEW,
                            sourceWidth = result.sourceDimensions.width,
                            sourceHeight = result.sourceDimensions.height,
                            corners = it.corners ?: result.corners,
                            initialCorners = it.initialCorners ?: result.corners,
                            isAutoDetected = result.isAutoDetected
                        )
                    }
                }
                val latest = requireSession().pages.first { it.id == page.id }
                val corners = latest.corners ?: result.corners
                val initial = latest.initialCorners ?: result.corners
                val dimensions = ImageDimensions(latest.sourceWidth, latest.sourceHeight)
                _uiState.value = ScannerUiState.CropReady(
                    session = requireSession(), page = latest, previewBitmap = result.previewBitmap,
                    corners = corners, initialDetectedCorners = initial,
                    isValidGeometry = CornerValidator.isValidQuadrilateral(corners.toArray(), dimensions)
                )
            }.onFailure {
                updatePage(page.id) { it.copy(status = SessionPageStatus.FAILED, failureMessage = "This image could not be opened. Try again or remove it.") }
                showReview()
            }
    }

    private fun openAcceptedPage(page: SessionPage) {
        val bitmap = page.processedPath?.let(sessionStore::readBitmap)
        if (bitmap == null) {
            updatePage(page.id) { it.copy(status = SessionPageStatus.CROP_REVIEW, processedPath = null, thumbnailPath = null) }
            viewModelScope.launch { openCrop(requireSession().pages.first { it.id == page.id }) }
            return
        }
        _uiState.value = ScannerUiState.PreviewReady(requireSession(), page, page.filter, bitmap)
    }

    private suspend fun recreateTreatmentPreview(page: SessionPage) {
        val corners = page.corners ?: return openCrop(page)
        _uiState.value = ScannerUiState.Processing(requireSession(), "Restoring page…")
        runCatching { scannerEngine.cropAndFilter(sourceUri(page), corners, page.filter) }
            .onSuccess { _uiState.value = ScannerUiState.PreviewReady(requireSession(), page, page.filter, it) }
            .onFailure { openCrop(page) }
    }

    private fun summaries(draft: ScanSession): List<SessionPageSummary> = draft.pages.sortedBy { it.position }.map { page ->
        SessionPageSummary(
            id = page.id,
            position = page.position,
            status = page.status,
            thumbnail = page.thumbnailPath?.let(sessionStore::readBitmap),
            message = page.failureMessage,
            canRetryDirectly = page.sourcePath != null
        )
    }

    private fun updatePage(pageId: String, transform: (SessionPage) -> SessionPage) {
        val draft = requireSession()
        updateSession(draft.copy(pages = draft.pages.map { if (it.id == pageId) transform(it) else it }))
    }

    private fun updateSession(updated: ScanSession) {
        val timestamped = updated.copy(updatedAtEpochMillis = System.currentTimeMillis())
        sessionStore.save(timestamped)
        session = timestamped
    }

    private fun requireSession(): ScanSession = checkNotNull(session)
    private fun sourceUri(page: SessionPage): Uri = Uri.fromFile(sessionFile(page.sourcePath))
    private fun sessionFile(path: String?): File = sessionStore.resolve(checkNotNull(path))

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val maxSize = 360
        if (bitmap.width <= maxSize && bitmap.height <= maxSize) return bitmap
        val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }
}

data class CameraCaptureState(
    val capturedCount: Int = 0,
    val isProcessing: Boolean = false,
    val message: String? = null,
    val mode: CameraScanMode = CameraScanMode.Document
)

private fun fullFrameCorners(dimensions: ImageDimensions) = CornerPoints(
    Point(1.0, 1.0),
    Point(dimensions.width - 2.0, 1.0),
    Point(dimensions.width - 2.0, dimensions.height - 2.0),
    Point(1.0, dimensions.height - 2.0)
)

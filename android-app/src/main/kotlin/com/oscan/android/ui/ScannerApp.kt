package com.oscan.android.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.session.ScanSession
import com.oscan.android.data.session.SessionPageStatus
import com.oscan.android.data.storage.DocumentFileStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerApp(
    viewModel: ScannerViewModel,
    cameraViewModel: CameraViewModel,
    libraryViewModel: LibraryViewModel,
    fileStore: DocumentFileStore,
    scannerEngine: com.oscan.android.engine.ScannerEngine? = null,
    repository: com.oscan.android.data.repository.DocumentRepository? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    var replacementPageId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showCamera by rememberSaveable { mutableStateOf(false) }
    val captureState by viewModel.cameraCaptureState.collectAsState()

    val multiplePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        viewModel.onImagesSelected(uris)
    }
    val replacementPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        replacementPageId?.let { viewModel.onReplacementSelected(it, uri) }
        replacementPageId = null
    }
    val launchMultiplePicker = {
        showCamera = false
        multiplePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val launchReplacement: (String) -> Unit = { pageId ->
        replacementPageId = pageId
        replacementPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    if (uiState is ScannerUiState.Empty) {
        if (showCamera) {
            BackHandler {
                if (!captureState.isProcessing) {
                    showCamera = false
                    if (captureState.capturedCount > 0) viewModel.finishCameraCapture()
                }
            }
            LiveCameraScreen(
                cameraViewModel = cameraViewModel,
                captureState = captureState,
                onCaptured = viewModel::onCameraCaptured,
                onDone = { showCamera = false; viewModel.finishCameraCapture() },
                onClose = { showCamera = false; if (captureState.capturedCount > 0) viewModel.finishCameraCapture() },
                onImport = launchMultiplePicker,
                shutterFeedbackEnabled = libraryState.userPreferences.shutterFeedback
            )
            return
        }
        OScanAppShell(
            libraryViewModel = libraryViewModel,
            scannerViewModel = viewModel,
            scannerEngine = scannerEngine,
            repository = repository,
            fileStore = fileStore,
            onImportImages = launchMultiplePicker,
            onOpenCamera = { showCamera = true }
        )
        return
    }

    val state = uiState
    val session = state.sessionOrNull()
    val hasAcceptedPages = session?.acceptedPages?.isNotEmpty() == true

    fun requestDiscard() {
        if (hasAcceptedPages) showDiscardDialog = true else viewModel.discardSession()
    }

    BackHandler(enabled = state !is ScannerUiState.Empty) {
        when (state) {
            is ScannerUiState.CropReady -> if (hasAcceptedPages) viewModel.showReview() else requestDiscard()
            is ScannerUiState.Review -> requestDiscard()
            is ScannerUiState.PreviewReady -> viewModel.onBackToCrop()
            is ScannerUiState.SaveDocument -> viewModel.showReview()
            is ScannerUiState.Error -> viewModel.dismissError()
            is ScannerUiState.Saved -> viewModel.startAnother()
            else -> Unit
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard this scan?") },
            text = { Text("Accepted pages will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; viewModel.discardSession() }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") } }
        )
    }

    val title = when (state) {
        is ScannerUiState.CropReady -> "Adjust edges"
        is ScannerUiState.PreviewReady -> "Enhance page"
        is ScannerUiState.Review -> "Review pages"
        is ScannerUiState.SaveDocument -> "Save document"
        is ScannerUiState.Saved -> "Document saved"
        else -> "OScan"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (state is ScannerUiState.CropReady || state is ScannerUiState.PreviewReady ||
                        state is ScannerUiState.Review || state is ScannerUiState.SaveDocument
                    ) {
                        IconButton(onClick = {
                            when (state) {
                                is ScannerUiState.PreviewReady -> viewModel.onBackToCrop()
                                is ScannerUiState.SaveDocument -> viewModel.showReview()
                                is ScannerUiState.CropReady -> if (hasAcceptedPages) viewModel.showReview() else requestDiscard()
                                else -> requestDiscard()
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val announcement = when (state) {
                ScannerUiState.LoadingSession -> "Restoring scan"
                is ScannerUiState.CropReady -> "Page ready. Adjust the crop boundary."
                is ScannerUiState.Processing -> state.message
                is ScannerUiState.PreviewReady -> "Processing complete. Choose a treatment."
                is ScannerUiState.Review -> "Review ready. ${state.session.acceptedPages.size} pages accepted."
                is ScannerUiState.SaveDocument -> state.errorMessage ?: if (state.isSaving) "Saving document" else "Ready to save document"
                is ScannerUiState.Saved -> "Document saved locally"
                is ScannerUiState.Error -> state.message
                is ScannerUiState.Importing -> if (state.completed == state.total) "Import complete" else ""
                ScannerUiState.Empty -> ""
            }
            if (announcement.isNotEmpty()) {
                Box(
                    Modifier.size(1.dp).semantics {
                        liveRegion = if (state is ScannerUiState.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
                        contentDescription = announcement
                    }
                )
            }
            when (state) {
                ScannerUiState.Empty -> Unit
                ScannerUiState.LoadingSession -> ProgressState("Restoring your scan…")
                is ScannerUiState.Importing -> ProgressState("Importing images ${state.completed} of ${state.total}…")
                is ScannerUiState.Processing -> ProgressState(state.message)
                is ScannerUiState.CropReady -> Column(Modifier.fillMaxSize()) {
                    SessionPositionHeader(state.session, state.page.position, state.previewBitmap)
                    CropScreen(
                        previewBitmap = state.previewBitmap,
                        sourceDimensions = com.oscan.core.model.ImageDimensions(state.page.sourceWidth, state.page.sourceHeight),
                        corners = state.corners,
                        isAutoDetected = state.page.isAutoDetected,
                        isValidGeometry = state.isValidGeometry,
                        onCornerMoved = viewModel::onCornerMoved,
                        onReset = viewModel::onResetCorners,
                        onRetake = { launchReplacement(state.page.id) },
                        onCropConfirmed = viewModel::onCropConfirmed
                    )
                }
                is ScannerUiState.PreviewReady -> Column(Modifier.fillMaxSize()) {
                    SessionPositionHeader(state.session, state.page.position, state.croppedBitmap)
                    PreviewScreen(
                        croppedBitmap = state.croppedBitmap,
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = viewModel::onFilterSelected,
                        onBackToCrop = viewModel::onBackToCrop,
                        onExportPdfRequested = viewModel::acceptCurrentPage
                    )
                }
                is ScannerUiState.Review -> SessionReviewScreen(
                    state = state,
                    onOpen = viewModel::openPage,
                    onRetry = viewModel::retryPage,
                    onReplace = launchReplacement,
                    onRemove = viewModel::removePage,
                    onMove = viewModel::movePage,
                    onAdd = launchMultiplePicker,
                    onFinish = viewModel::beginFinish
                )
                is ScannerUiState.SaveDocument -> SaveDocumentScreen(
                    state = state,
                    onNameChanged = viewModel::updateDocumentName,
                    onFolderSelected = viewModel::selectFolder,
                    onSave = viewModel::saveDocument
                )
                is ScannerUiState.Saved -> SavedDocumentScreen(
                    state = state,
                    onOpenDocument = { id ->
                        libraryViewModel.openDocument(id)
                        viewModel.startAnother()
                    },
                    onDone = viewModel::startAnother
                )
                is ScannerUiState.Error -> ErrorState(state.message, viewModel::dismissError)
            }
        }
    }
}

@Composable
private fun SessionPositionHeader(session: ScanSession, position: Int, thumbnail: android.graphics.Bitmap) {
    Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(thumbnail.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Page ${position + 1} of ${session.pages.size}", fontWeight = FontWeight.SemiBold)
                Text("${session.acceptedPages.size} accepted", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SessionReviewScreen(
    state: ScannerUiState.Review,
    onOpen: (String) -> Unit,
    onRetry: (String) -> Unit,
    onReplace: (String) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onAdd: () -> Unit,
    onFinish: () -> Unit
) {
    val pending = state.pages.any { it.status == SessionPageStatus.CROP_REVIEW || it.status == SessionPageStatus.TREATMENT_REVIEW }
    Column(Modifier.fillMaxSize()) {
        Text(
            "${state.session.acceptedPages.size} of ${state.pages.size} pages accepted",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.pages, key = SessionPageSummary::id) { page ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page.thumbnail != null) {
                        Image(page.thumbnail.asImageBitmap(), "Page ${page.position + 1} thumbnail", Modifier.size(56.dp))
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null) }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Page ${page.position + 1}", fontWeight = FontWeight.SemiBold)
                        Text(page.status.readableLabel(), style = MaterialTheme.typography.bodySmall)
                        page.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            when (page.status) {
                                SessionPageStatus.ACCEPTED, SessionPageStatus.CROP_REVIEW, SessionPageStatus.TREATMENT_REVIEW ->
                                    TextButton(onClick = { onOpen(page.id) }) { Text(if (page.status == SessionPageStatus.ACCEPTED) "Edit" else "Review") }
                                SessionPageStatus.FAILED -> TextButton(onClick = {
                                    if (page.canRetryDirectly) onRetry(page.id) else onReplace(page.id)
                                }) { Text(if (page.canRetryDirectly) "Try again" else "Choose again") }
                                else -> Unit
                            }
                            TextButton(onClick = { onRemove(page.id) }) { Text("Remove") }
                        }
                    }
                    Column {
                        IconButton(onClick = { onMove(page.id, -1) }, enabled = page.position > 0) {
                            Icon(Icons.Default.ArrowUpward, "Move page up")
                        }
                        IconButton(onClick = { onMove(page.id, 1) }, enabled = page.position < state.pages.lastIndex) {
                            Icon(Icons.Default.ArrowDownward, "Move page down")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
        AdaptiveActionGroup(Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedButton(onClick = onAdd) { Text("Add pages") }
            Button(onClick = onFinish, enabled = state.session.acceptedPages.isNotEmpty() && !pending) { Text("Finish") }
        }
    }
}

@Composable
private fun SaveDocumentScreen(
    state: ScannerUiState.SaveDocument,
    onNameChanged: (String) -> Unit,
    onFolderSelected: (FolderId?) -> Unit,
    onSave: () -> Unit
) {
    var folderMenuOpen by remember { mutableStateOf(false) }
    val selectedFolder = state.folders.find { it.id.value == state.session.selectedFolderId }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Save ${state.session.acceptedPages.size} pages as one local document.")
        TextField(
            value = state.session.documentName,
            onValueChange = onNameChanged,
            label = { Text("Document name") },
            enabled = !state.isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.folders.isNotEmpty()) {
            Box {
                OutlinedButton(onClick = { folderMenuOpen = true }, enabled = !state.isSaving) {
                    Text(selectedFolder?.name ?: "No folder")
                }
                androidx.compose.material3.DropdownMenu(expanded = folderMenuOpen, onDismissRequest = { folderMenuOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("No folder") },
                        onClick = { folderMenuOpen = false; onFolderSelected(null) }
                    )
                    state.folders.forEach { folder ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = { folderMenuOpen = false; onFolderSelected(folder.id) }
                        )
                    }
                }
            }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSave,
            enabled = !state.isSaving && state.session.documentName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (state.isSaving) "Saving…" else "Save document")
        }
    }
}

@Composable
private fun SavedDocumentScreen(
    state: ScannerUiState.Saved,
    onOpenDocument: (com.oscan.android.data.model.DocumentId) -> Unit,
    onDone: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Saved locally", style = MaterialTheme.typography.titleLarge)
        Text("${state.name} • ${state.pageCount} ${if (state.pageCount == 1) "page" else "pages"}")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onOpenDocument(state.documentId) }, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Open document")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Done")
        }
    }
}

@Composable
private fun ProgressState(message: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDismiss) { Text("OK") }
    }
}

private fun ScannerUiState.sessionOrNull(): ScanSession? = when (this) {
    is ScannerUiState.Importing -> session
    is ScannerUiState.CropReady -> session
    is ScannerUiState.Processing -> session
    is ScannerUiState.PreviewReady -> session
    is ScannerUiState.Review -> session
    is ScannerUiState.SaveDocument -> session
    else -> null
}

private fun SessionPageStatus.readableLabel(): String = when (this) {
    SessionPageStatus.ACCEPTED -> "Accepted"
    SessionPageStatus.CROP_REVIEW, SessionPageStatus.TREATMENT_REVIEW -> "Needs review"
    SessionPageStatus.FAILED -> "Needs attention"
    SessionPageStatus.IMPORTING -> "Importing"
    SessionPageStatus.DETECTING -> "Finding edges"
    SessionPageStatus.PROCESSING -> "Processing"
}

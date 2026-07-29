package com.oscan.android.ui

import com.oscan.android.R

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.session.ScanSession
import com.oscan.android.data.session.SessionPage
import com.oscan.android.data.session.SessionPageStatus
import com.oscan.android.data.storage.DocumentFileStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerApp(
    viewModel: ScannerViewModel,
    cameraViewModel: CameraViewModel,
    libraryViewModel: LibraryViewModel,
    appLocaleController: com.oscan.android.localization.AppLocaleController? = null,
    vaultViewModel: com.oscan.android.ui.vault.VaultViewModel? = null,
    fileStore: DocumentFileStore,
    scannerEngine: com.oscan.android.engine.ScannerEngine? = null,
    repository: com.oscan.android.data.repository.DocumentRepository? = null,
    onSecureWindowChanged: ((Boolean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraUiState by cameraViewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    var replacementPageId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showAddPagesDialog by rememberSaveable { mutableStateOf(false) }
    var addingPageWithCamera by rememberSaveable { mutableStateOf(false) }
    val captureState by viewModel.cameraCaptureState.collectAsState()

    DisposableEffect(viewModel) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.discardIncompleteIdCardCapture()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val multiplePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        viewModel.onImagesSelected(uris)
    }
    val replacementPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        replacementPageId?.let { viewModel.onReplacementSelected(it, uri) }
        replacementPageId = null
    }
    val launchMultiplePicker = {
        multiplePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val launchReplacement: (String) -> Unit = { pageId ->
        replacementPageId = pageId
        replacementPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    if (uiState is ScannerUiState.Empty) {
        OScanAppShell(
            libraryViewModel = libraryViewModel,
            appLocaleController = appLocaleController,
            vaultViewModel = vaultViewModel,
            cameraViewModel = cameraViewModel,
            captureState = captureState,
            onCaptured = viewModel::onCameraCaptured,
            onDone = viewModel::finishCameraCapture,
            onAbandonIncompleteIdCard = viewModel::discardIncompleteIdCardCapture,
            scannerViewModel = viewModel,
            scannerEngine = scannerEngine,
            repository = repository,
            fileStore = fileStore,
            onImportImages = launchMultiplePicker,
            onSecureWindowChanged = onSecureWindowChanged
        )
        return
    }

    val state = uiState
    val session = state.sessionOrNull()
    val hasAcceptedPages = session?.acceptedPages?.isNotEmpty() == true

    if (addingPageWithCamera) {
        val cameraIsBusy = cameraUiState.isCapturing || captureState.isProcessing
        val finishAdditionalCapture = {
            if (!cameraIsBusy) {
                addingPageWithCamera = false
                viewModel.finishAdditionalCameraCapture()
            }
        }
        BackHandler { finishAdditionalCapture() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.action_take_picture)) },
                    navigationIcon = {
                        IconButton(
                            onClick = finishAdditionalCapture,
                            enabled = !cameraIsBusy
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back_button)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                LiveCameraScreen(
                    cameraViewModel = cameraViewModel,
                    captureState = captureState,
                    onCaptured = viewModel::onCameraCaptured,
                    onDone = finishAdditionalCapture,
                    onAbandonIncompleteIdCard = {},
                    onImport = {},
                    shutterFeedbackEnabled = libraryUiState.userPreferences.shutterFeedback,
                    allowModeSelection = false,
                    showImport = false
                )
            }
        }
        return
    }

    fun requestDiscard() {
        if (hasAcceptedPages) showDiscardDialog = true else viewModel.discardSession()
    }

    BackHandler(enabled = state !is ScannerUiState.Empty && state !is ScannerUiState.PreviewReady) {
        when (state) {
            is ScannerUiState.CropReady -> if (hasAcceptedPages) {
                if (!viewModel.discardPendingAddedPage()) viewModel.showReview()
            } else requestDiscard()
            is ScannerUiState.IdCardAdjust -> requestDiscard()
            is ScannerUiState.Review -> requestDiscard()
            is ScannerUiState.SaveDocument -> viewModel.showReview()
            is ScannerUiState.Error -> viewModel.dismissError()
            is ScannerUiState.Saved -> viewModel.startAnother()
            else -> Unit
        }
    }

    // Keep preview navigation in a dedicated handler. The ID-card preview replaces
    // its two source pages with a generated sheet, so system Back must use the same
    // ID-card-aware route as the toolbar action instead of a stale crop callback.
    BackHandler(enabled = state is ScannerUiState.PreviewReady) {
        viewModel.onBackToCrop()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.scanner_discard_title)) },
            text = { Text(stringResource(R.string.scanner_discard_body)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; viewModel.discardSession() }) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.action_keep_editing)) } }
        )
    }

            val title = when (state) {
                is ScannerUiState.CropReady -> stringResource(R.string.scanner_adjust_edges)
                is ScannerUiState.IdCardAdjust -> stringResource(R.string.id_card_adjust_both)
                is ScannerUiState.PreviewReady -> stringResource(R.string.scanner_enhance_page)
                is ScannerUiState.Review -> stringResource(R.string.scanner_review_pages)
                is ScannerUiState.SaveDocument -> stringResource(R.string.scanner_save_document)
                is ScannerUiState.Saved -> stringResource(R.string.scanner_document_saved)
                else -> stringResource(R.string.app_name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (state is ScannerUiState.CropReady || state is ScannerUiState.IdCardAdjust || state is ScannerUiState.PreviewReady ||
                        state is ScannerUiState.Review || state is ScannerUiState.SaveDocument
                    ) {
                        IconButton(onClick = {
                            when (state) {
                                is ScannerUiState.PreviewReady -> viewModel.onBackToCrop()
                                is ScannerUiState.SaveDocument -> viewModel.showReview()
                                is ScannerUiState.CropReady -> if (hasAcceptedPages) {
                                    if (!viewModel.discardPendingAddedPage()) viewModel.showReview()
                                } else requestDiscard()
                                is ScannerUiState.IdCardAdjust -> requestDiscard()
                                else -> requestDiscard()
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button)) }
                    }
                },
                actions = {
                    if (state is ScannerUiState.CropReady) {
                        IconButton(onClick = viewModel::onResetCorners) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.scanner_reset_edges))
                        }
                        TextButton(
                            onClick = viewModel::onCropConfirmed,
                            enabled = state.isValidGeometry
                        ) { Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold) }
                    } else if (state is ScannerUiState.IdCardAdjust) {
                        IconButton(onClick = viewModel::resetIdCardCorners) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.id_card_reset_both))
                        }
                        TextButton(
                            onClick = viewModel::confirmIdCardAdjustment,
                            enabled = state.isFrontValid && state.isBackValid
                        ) { Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold) }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val announcement = when (state) {
                ScannerUiState.LoadingSession -> stringResource(R.string.scanner_restoring)
                is ScannerUiState.CropReady -> stringResource(R.string.scanner_crop_ready)
                is ScannerUiState.IdCardAdjust -> stringResource(R.string.id_card_adjust_announcement)
                is ScannerUiState.Processing -> localizedRuntimeMessage(state.message)
                is ScannerUiState.PreviewReady -> stringResource(R.string.scanner_processing_complete)
                is ScannerUiState.Review -> stringResource(R.string.scanner_review_ready, state.session.acceptedPages.size)
                is ScannerUiState.SaveDocument -> state.errorMessage?.let { localizedRuntimeMessage(it) }
                    ?: if (state.isSaving) stringResource(R.string.scan_saving_document) else stringResource(R.string.scan_ready_save)
                is ScannerUiState.Saved -> stringResource(R.string.scanner_saved_announcement)
                is ScannerUiState.Error -> localizedRuntimeMessage(state.message)
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
                ScannerUiState.LoadingSession -> ProgressState(stringResource(R.string.scanner_restoring_progress))
                is ScannerUiState.Importing -> ProgressState(stringResource(R.string.scanner_importing_progress, state.completed, state.total))
                is ScannerUiState.Processing -> ProgressState(localizedRuntimeMessage(state.message))
                is ScannerUiState.CropReady -> Column(Modifier.fillMaxSize()) {
                    SessionPositionHeader(state.session, state.page.position, state.previewBitmap)
                    CropScreen(
                        previewBitmap = state.previewBitmap,
                        sourceDimensions = com.oscan.core.model.ImageDimensions(state.page.sourceWidth, state.page.sourceHeight),
                        corners = state.corners,
                        isValidGeometry = state.isValidGeometry,
                        onCornerMoved = viewModel::onCornerMoved
                    )
                }
                is ScannerUiState.IdCardAdjust -> IdCardAdjustmentScreen(
                    state = state,
                    onCornerMoved = viewModel::onIdCardCornerMoved
                )
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
                    onAdd = { showAddPagesDialog = true },
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
                is ScannerUiState.Error -> ErrorState(localizedRuntimeMessage(state.message), viewModel::dismissError)
            }
        }
    }

    if (showAddPagesDialog && state is ScannerUiState.Review) {
        AlertDialog(
            onDismissRequest = { showAddPagesDialog = false },
            title = { Text(stringResource(R.string.action_add_pages)) },
            text = { Text(stringResource(R.string.dialog_add_pages_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showAddPagesDialog = false
                    viewModel.beginAdditionalCameraCapture()
                    addingPageWithCamera = true
                }) { Text(stringResource(R.string.action_take_picture)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddPagesDialog = false
                    launchMultiplePicker()
                }) { Text(stringResource(R.string.action_import_images)) }
            }
        )
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
                Text(stringResource(R.string.page_position_format, position + 1, session.pages.size), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.scanner_accepted_count, session.acceptedPages.size), style = MaterialTheme.typography.bodySmall)
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
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.scanner_review_count, state.session.acceptedPages.size, state.pages.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.pages, key = SessionPageSummary::id) { page ->
                var rowHeightPx by remember(page.id) { mutableStateOf(1) }
                val moveUpLabel = stringResource(R.string.cd_move_page_up)
                val moveDownLabel = stringResource(R.string.cd_move_page_down)
                val reorderDescription = stringResource(R.string.cd_drag_to_reorder_page, page.position + 1)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { rowHeightPx = it.height.coerceAtLeast(1) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page.thumbnail != null) {
                        Image(page.thumbnail.asImageBitmap(), stringResource(R.string.cd_page_thumbnail, page.position + 1), Modifier.size(56.dp))
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null) }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.scanner_page_number, page.position + 1), fontWeight = FontWeight.SemiBold)
                        Text(page.status.readableLabel(), style = MaterialTheme.typography.bodySmall)
                        page.message?.let { Text(localizedRuntimeMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            when (page.status) {
                                SessionPageStatus.ACCEPTED, SessionPageStatus.CROP_REVIEW, SessionPageStatus.TREATMENT_REVIEW ->
                                    TextButton(onClick = { onOpen(page.id) }) {
                                        Text(
                                            if (page.status == SessionPageStatus.ACCEPTED) {
                                                stringResource(R.string.action_edit)
                                            } else {
                                                stringResource(R.string.action_review)
                                            }
                                        )
                                    }
                                SessionPageStatus.FAILED -> TextButton(onClick = {
                                    if (page.canRetryDirectly) onRetry(page.id) else onReplace(page.id)
                                }) {
                                    Text(
                                        if (page.canRetryDirectly) {
                                            stringResource(R.string.action_try_again)
                                        } else {
                                            stringResource(R.string.action_choose_again)
                                        }
                                    )
                                }
                                else -> Unit
                            }
                            TextButton(onClick = { onRemove(page.id) }) { Text(stringResource(R.string.action_remove)) }
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = reorderDescription
                                role = Role.Button
                                customActions = buildList {
                                    if (page.position > 0) {
                                        add(CustomAccessibilityAction(moveUpLabel) {
                                            onMove(page.id, -1)
                                            true
                                        })
                                    }
                                    if (page.position < state.pages.lastIndex) {
                                        add(CustomAccessibilityAction(moveDownLabel) {
                                            onMove(page.id, 1)
                                            true
                                        })
                                    }
                                }
                            }
                            .pointerInput(page.id, rowHeightPx) {
                                var accumulatedDragY = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragCancel = { accumulatedDragY = 0f },
                                    onDragEnd = { accumulatedDragY = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedDragY += dragAmount.y
                                        val threshold = (rowHeightPx * 0.45f).coerceAtLeast(1f)
                                        while (accumulatedDragY >= threshold) {
                                            onMove(page.id, 1)
                                            accumulatedDragY -= threshold
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        while (accumulatedDragY <= -threshold) {
                                            onMove(page.id, -1)
                                            accumulatedDragY += threshold
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                )
                            }
                    ) {
                        Icon(Icons.Default.DragHandle, contentDescription = null)
                    }
                }
                HorizontalDivider()
            }
        }
        AdaptiveActionGroup(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            OutlinedButton(onClick = onAdd) { Text(stringResource(R.string.action_add_pages)) }
            Button(onClick = onFinish, enabled = state.session.acceptedPages.isNotEmpty() && !pending) { Text(stringResource(R.string.action_finish)) }
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
        Text(stringResource(R.string.scanner_save_summary, state.session.acceptedPages.size))
        TextField(
            value = state.session.documentName,
            onValueChange = onNameChanged,
            label = { Text(stringResource(R.string.scanner_document_name)) },
            enabled = !state.isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.folders.isNotEmpty()) {
            Box {
                OutlinedButton(onClick = { folderMenuOpen = true }, enabled = !state.isSaving) {
                    Text(selectedFolder?.name ?: stringResource(R.string.scanner_no_folder))
                }
                androidx.compose.material3.DropdownMenu(expanded = folderMenuOpen, onDismissRequest = { folderMenuOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.scanner_no_folder)) },
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
        state.errorMessage?.let { Text(localizedRuntimeMessage(it), color = MaterialTheme.colorScheme.error) }
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
            Text(
                if (state.isSaving) {
                    stringResource(R.string.action_saving)
                } else {
                    stringResource(R.string.scanner_save_document)
                }
            )
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
        Text(stringResource(R.string.scanner_saved_locally), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.scanner_saved_summary, state.name, state.pageCount))
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onOpenDocument(state.documentId) }, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text(stringResource(R.string.action_open_document))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text(stringResource(R.string.action_done))
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
        Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
    }
}

private fun previewScanSession(): ScanSession {
    val pages = listOf(
        SessionPage(id = "preview-1", position = 0, status = SessionPageStatus.ACCEPTED),
        SessionPage(id = "preview-2", position = 1, status = SessionPageStatus.ACCEPTED)
    )
    return ScanSession(id = "preview-session", pages = pages, documentName = "July expense report")
}

@OScanPagePreview
@Composable
internal fun ScanReviewPagePreview() = com.oscan.android.ui.theme.OScanTheme {
    val session = previewScanSession()
    SessionReviewScreen(
        state = ScannerUiState.Review(
            session,
            session.pages.map { page ->
                SessionPageSummary(page.id, page.position, page.status, null, null, true)
            }
        ),
        onOpen = {}, onRetry = {}, onReplace = {}, onRemove = {}, onMove = { _, _ -> }, onAdd = {}, onFinish = {}
    )
}

@OScanPagePreview
@Composable
internal fun SaveDocumentPagePreview() = com.oscan.android.ui.theme.OScanTheme {
    SaveDocumentScreen(
        state = ScannerUiState.SaveDocument(
            previewScanSession(),
            listOf(Folder(FolderId("preview-folder"), "Receipts", java.time.Instant.now(), java.time.Instant.now()))
        ),
        onNameChanged = {}, onFolderSelected = {}, onSave = {}
    )
}

@OScanPagePreview
@Composable
internal fun SavedDocumentPagePreview() = com.oscan.android.ui.theme.OScanTheme {
    SavedDocumentScreen(ScannerUiState.Saved(DocumentId("preview-document"), "July expense report", 2), {}, {})
}

@OScanPagePreview
@Composable
internal fun ScannerProgressPagePreview() = com.oscan.android.ui.theme.OScanTheme {
    ProgressState("Enhancing page 2 of 3…")
}

@OScanPagePreview
@Composable
internal fun ScannerErrorPagePreview() = com.oscan.android.ui.theme.OScanTheme {
    ErrorState("This image could not be processed.", {})
}

private fun ScannerUiState.sessionOrNull(): ScanSession? = when (this) {
    is ScannerUiState.Importing -> session
    is ScannerUiState.CropReady -> session
    is ScannerUiState.IdCardAdjust -> session
    is ScannerUiState.Processing -> session
    is ScannerUiState.PreviewReady -> session
    is ScannerUiState.Review -> session
    is ScannerUiState.SaveDocument -> session
    else -> null
}

@Composable
private fun SessionPageStatus.readableLabel(): String = when (this) {
    SessionPageStatus.ACCEPTED -> stringResource(R.string.status_accepted)
    SessionPageStatus.CROP_REVIEW, SessionPageStatus.TREATMENT_REVIEW -> stringResource(R.string.status_needs_review)
    SessionPageStatus.FAILED -> stringResource(R.string.status_needs_attention)
    SessionPageStatus.IMPORTING -> stringResource(R.string.status_importing)
    SessionPageStatus.DETECTING -> stringResource(R.string.status_finding_edges)
    SessionPageStatus.PROCESSING -> stringResource(R.string.status_processing)
}

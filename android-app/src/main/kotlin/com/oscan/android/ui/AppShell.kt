package com.oscan.android.ui

import java.io.File
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.oscan.android.R
import com.oscan.android.data.preferences.AppLanguage
import com.oscan.android.localization.AppLocaleController
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Scanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.oscan.android.ui.vault.VaultFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.model.Page
import com.oscan.android.data.model.PageId
import com.oscan.android.data.preferences.DocumentSort
import com.oscan.android.data.preferences.LibraryPresentation
import com.oscan.android.data.storage.DocumentFileStore
import kotlinx.coroutines.launch

private enum class AppDestination(val selectedIcon: ImageVector, val icon: ImageVector) {
    Home(Icons.Filled.Home, Icons.Outlined.Home),
    Scan(Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    Me(Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
private fun AppDestination.label(): String = when (this) {
    AppDestination.Home -> stringResource(R.string.nav_library)
    AppDestination.Scan -> stringResource(R.string.nav_camera)
    AppDestination.Me -> stringResource(R.string.nav_me)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OScanAppShell(
    libraryViewModel: LibraryViewModel,
    appLocaleController: AppLocaleController? = null,
    vaultViewModel: com.oscan.android.ui.vault.VaultViewModel? = null,
    cameraViewModel: CameraViewModel? = null,
    captureState: CameraCaptureState? = null,
    onCaptured: ((File) -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    scannerViewModel: ScannerViewModel? = null,
    scannerEngine: com.oscan.android.engine.ScannerEngine? = null,
    repository: com.oscan.android.data.repository.DocumentRepository? = null,
    fileStore: DocumentFileStore,
    onImportImages: () -> Unit,
    onSecureWindowChanged: ((Boolean) -> Unit)? = null
) {
    val state by libraryViewModel.uiState.collectAsState()
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    val destinationPagerState = rememberPagerState(
        initialPage = destination.ordinal,
        pageCount = { AppDestination.entries.size }
    )
    val navigationScope = rememberCoroutineScope()
    val selectDestination: (AppDestination) -> Unit = { target ->
        destination = target
        navigationScope.launch { destinationPagerState.animateScrollToPage(target.ordinal) }
    }
    LaunchedEffect(destinationPagerState.settledPage) {
        destination = AppDestination.entries[destinationPagerState.settledPage]
    }
    var viewerPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingPage by remember { mutableStateOf<com.oscan.android.data.model.Page?>(null) }
    var addPagesDialogOpen by remember { mutableStateOf(false) }
    var isViewingFolders by rememberSaveable { mutableStateOf(false) }
    var isViewingVault by rememberSaveable { mutableStateOf(false) }
    var pendingVaultMoveDocumentIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var activeSubRoute by rememberSaveable { mutableStateOf(SettingsSubRoute.NONE) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val localizedMessage = state.message?.let { localizedRuntimeMessage(it) }

    LaunchedEffect(localizedMessage) {
        localizedMessage?.let {
            snackbarHostState.showSnackbar(it)
            libraryViewModel.clearMessage()
        }
    }

    if (isViewingVault && vaultViewModel != null) {
        DisposableEffect(onSecureWindowChanged) {
            onSecureWindowChanged?.invoke(true)
            onDispose { onSecureWindowChanged?.invoke(false) }
        }
        VaultFlow(
            viewModel = vaultViewModel,
            documentsToMoveIntoVault = pendingVaultMoveDocumentIds.mapNotNull { id ->
                state.documents.find { it.id.value == id }
            },
            onMoveIntoVaultConsumed = { pendingVaultMoveDocumentIds = emptyList() },
            onExit = {
                pendingVaultMoveDocumentIds = emptyList()
                isViewingVault = false
            }
        )
        return
    }

    if (activeSubRoute != SettingsSubRoute.NONE) {
        BackHandler { activeSubRoute = SettingsSubRoute.NONE }
        val context = androidx.compose.ui.platform.LocalContext.current
        when (activeSubRoute) {
            SettingsSubRoute.CAPTURE -> CaptureSettingsScreen(
                preferences = state.userPreferences,
                onAutoCaptureChanged = libraryViewModel::setAutoCaptureDefault,
                onShutterFeedbackChanged = libraryViewModel::setShutterFeedback,
                onCameraLensChanged = libraryViewModel::setCameraLensPreference,
                onBack = { activeSubRoute = SettingsSubRoute.NONE }
            )
            SettingsSubRoute.ENHANCEMENT -> EnhancementSettingsScreen(
                preferences = state.userPreferences,
                onDefaultTreatmentChanged = libraryViewModel::setDefaultTreatment,
                onBack = { activeSubRoute = SettingsSubRoute.NONE }
            )
            SettingsSubRoute.EXPORT -> ExportSettingsScreen(
                preferences = state.userPreferences,
                onFilenamePatternChanged = libraryViewModel::setDefaultExportFilenamePattern,
                onPageSizeChanged = libraryViewModel::setDefaultPageSize,
                onJpegQualityChanged = libraryViewModel::setDefaultJpegQuality,
                onBack = { activeSubRoute = SettingsSubRoute.NONE }
            )
            SettingsSubRoute.APPEARANCE -> AppearanceSettingsScreen(
                themeChoice = state.userPreferences.themeChoice,
                accentTheme = state.userPreferences.accentTheme,
                onThemeChoiceSelected = libraryViewModel::setThemeChoice,
                onAccentThemeSelected = libraryViewModel::setAccentTheme,
                onBack = { activeSubRoute = SettingsSubRoute.NONE }
            )
            SettingsSubRoute.LANGUAGE -> LanguageSettingsScreen(
                selectedLanguage = appLocaleController?.currentLanguage() ?: AppLanguage.SYSTEM,
                onLanguageSelected = { lang ->
                    appLocaleController?.setLanguage(lang)
                },
                onBackClick = { activeSubRoute = SettingsSubRoute.NONE }
            )
            SettingsSubRoute.STORAGE -> StorageSettingsScreen(
                fileStore = fileStore,
                onCleanCache = { libraryViewModel.cleanCache(context, fileStore) },
                onBack = { activeSubRoute = SettingsSubRoute.NONE }
            )
            SettingsSubRoute.PRIVACY -> PrivacyScreen(onBack = { activeSubRoute = SettingsSubRoute.NONE })
            SettingsSubRoute.HELP -> HelpScreen(onBack = { activeSubRoute = SettingsSubRoute.NONE })
            SettingsSubRoute.ABOUT -> AboutScreen(onBack = { activeSubRoute = SettingsSubRoute.NONE })
            SettingsSubRoute.NONE -> Unit
        }
        return
    }

    if (editingPage != null && state.selectedDocumentId != null && scannerEngine != null && repository != null) {
        val targetDocId = state.selectedDocumentId!!
        val targetPage = editingPage!!
        val pageEditorViewModel = remember(targetPage.id.value) {
            PageEditorViewModel(targetDocId, targetPage, fileStore, scannerEngine, repository)
        }
        BackHandler { editingPage = null }
        PageEditorScreen(
            viewModel = pageEditorViewModel,
            onDismiss = { editingPage = null }
        )
        return
    }

    if (viewerPage != null && state.selectedDocument != null) {
        BackHandler { viewerPage = null }
        PageViewerScreen(state.selectedDocument!!, viewerPage!!, fileStore) { viewerPage = null }
        return
    }

    if (state.selectedDocumentId != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        BackHandler { libraryViewModel.closeDocument() }
        val detailPane: @Composable () -> Unit = {
            DocumentDetailScreen(
                document = state.selectedDocument,
                requestedDocumentExists = state.documents.any { it.id == state.selectedDocumentId },
                folders = state.folders,
                fileStore = fileStore,
                snackbarHostState = snackbarHostState,
                isExporting = state.isExporting,
                onBack = libraryViewModel::closeDocument,
                onRename = libraryViewModel::rename,
                onFavorite = libraryViewModel::setFavorite,
                onMove = libraryViewModel::moveToFolder,
                onTrash = libraryViewModel::moveToTrash,
                defaultJpegQuality = state.userPreferences.defaultJpegQuality,
                onExport = { ctx, doc, uri, format, quality -> libraryViewModel.exportDocumentToUri(ctx, doc, fileStore, uri, format, quality) },
                onShare = { ctx, doc, format, quality -> libraryViewModel.shareDocument(ctx, doc, fileStore, format, quality, context::startActivity) },
                onOpenPage = { viewerPage = it },
                onReorderPages = { pageIds: List<PageId> -> state.selectedDocumentId?.let { libraryViewModel.reorderPages(it, pageIds) } },
                onRotatePage = { pageId: PageId, delta: Int -> state.selectedDocumentId?.let { libraryViewModel.rotatePage(it, pageId, delta) } },
                onDeletePage = { pageId: PageId -> state.selectedDocumentId?.let { libraryViewModel.deletePage(it, pageId) } },
                onAddPages = { addPagesDialogOpen = true },
                onEditPage = { page: Page -> editingPage = page },
                onMoveToVault = {
                    state.selectedDocument?.let { document ->
                        pendingVaultMoveDocumentIds = listOf(document.id.value)
                        libraryViewModel.closeDocument()
                        isViewingVault = true
                    }
                }
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize()) {
                    LibraryMasterPane(
                        state = state,
                        fileStore = fileStore,
                        gridState = gridState,
                        listState = listState,
                        libraryViewModel = libraryViewModel,
                        modifier = Modifier.widthIn(min = 320.dp, max = 400.dp).fillMaxHeight()
                    )
                    Surface(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.width(1.dp).fillMaxHeight()) {}
                    Box(Modifier.weight(1f).fillMaxHeight()) { detailPane() }
                }
            } else {
                detailPane()
            }
        }

        if (addPagesDialogOpen && state.selectedDocument != null && scannerViewModel != null) {
            val doc = state.selectedDocument!!
            AlertDialog(
                onDismissRequest = { addPagesDialogOpen = false },
                title = { Text(stringResource(R.string.dialog_add_pages_title, doc.name)) },
                text = { Text(stringResource(R.string.dialog_add_pages_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        addPagesDialogOpen = false
                        scannerViewModel.prepareAddPagesToDocument(doc.id, doc.name)
                        destination = AppDestination.Scan
                    }) {
                        Text(stringResource(R.string.action_open_camera))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        addPagesDialogOpen = false
                        scannerViewModel.prepareAddPagesToDocument(doc.id, doc.name)
                        onImportImages()
                    }) {
                        Text(stringResource(R.string.action_import_images))
                    }
                }
            )
        }

        return
    }

    if (state.isViewingTrash) {
        BackHandler { libraryViewModel.closeTrash() }
        TrashScreen(
            trashDocuments = state.trashDocuments,
            fileStore = fileStore,
            selectionMode = state.selectionMode,
            selectedDocumentIds = state.selectedDocumentIds,
            onToggleSelectionMode = libraryViewModel::toggleSelectionMode,
            onToggleDocumentSelection = libraryViewModel::toggleDocumentSelection,
            onRestoreDocument = libraryViewModel::restoreDocument,
            onPermanentlyDeleteDocument = libraryViewModel::permanentlyDeleteDocument,
            onRestoreMultiple = libraryViewModel::restoreMultiple,
            onPermanentlyDeleteMultiple = libraryViewModel::permanentlyDeleteMultiple,
            onEmptyTrash = libraryViewModel::emptyTrash,
            onBack = libraryViewModel::closeTrash
        )
        return
    }

    if (state.selectedFolderId != null) {
        val currentFolder = state.folders.find { it.id == state.selectedFolderId }
        if (currentFolder != null) {
            BackHandler { libraryViewModel.closeFolder() }
            FolderDetailScreen(
                folder = currentFolder,
                documents = state.documents,
                fileStore = fileStore,
                gridState = gridState,
                listState = listState,
                presentation = state.presentation,
                selectionMode = state.selectionMode,
                selectedDocumentIds = state.selectedDocumentIds,
                onOpenDocument = libraryViewModel::openDocument,
                onToggleSelectionMode = libraryViewModel::toggleSelectionMode,
                onToggleDocumentSelection = libraryViewModel::toggleDocumentSelection,
                onRenameFolder = { name -> libraryViewModel.renameFolder(currentFolder.id, name) },
                onDeleteFolder = { libraryViewModel.deleteFolder(currentFolder.id) },
                onBack = libraryViewModel::closeFolder
            )
            return
        }
    }

    if (isViewingFolders) {
        BackHandler { isViewingFolders = false }
        FolderOverviewScreen(
            folders = state.folders,
            documents = state.documents,
            onOpenFolder = { id -> isViewingFolders = false; libraryViewModel.openFolder(id) },
            onCreateFolder = libraryViewModel::createFolder,
            onRenameFolder = libraryViewModel::renameFolder,
            onDeleteFolder = libraryViewModel::deleteFolder,
            onBack = { isViewingFolders = false }
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                OScanNavigationRail(destination, selectDestination)
                DestinationScaffold(
                    destination = destination,
                    pagerState = destinationPagerState,
                    state = state,
                    fileStore = fileStore,
                    gridState = gridState,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    onDestinationSelected = selectDestination,
                    onImportImages = onImportImages,
                    cameraViewModel = cameraViewModel,
                    captureState = captureState,
                    onCaptured = onCaptured,
                    onDone = onDone,
                    onOpenFolders = { isViewingFolders = true },
                    onOpenVault = {
                        if (vaultViewModel != null) isViewingVault = true
                    },
                    onMoveSelectedToVault = {
                        if (state.selectedDocumentIds.isNotEmpty() && vaultViewModel != null) {
                            pendingVaultMoveDocumentIds = state.selectedDocumentIds.map { it.value }
                            libraryViewModel.clearSelection()
                            isViewingVault = true
                        }
                    },
                    onOpenSubRoute = { activeSubRoute = it },
                    libraryViewModel = libraryViewModel,
                    appLocaleController = appLocaleController,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DestinationScaffold(
                destination = destination,
                pagerState = destinationPagerState,
                state = state,
                fileStore = fileStore,
                gridState = gridState,
                listState = listState,
                snackbarHostState = snackbarHostState,
                onDestinationSelected = selectDestination,
                onImportImages = onImportImages,
                cameraViewModel = cameraViewModel,
                captureState = captureState,
                onCaptured = onCaptured,
                onDone = onDone,
                onOpenFolders = { isViewingFolders = true },
                onOpenVault = {
                    if (vaultViewModel != null) isViewingVault = true
                },
                onMoveSelectedToVault = {
                    if (state.selectedDocumentIds.isNotEmpty() && vaultViewModel != null) {
                        pendingVaultMoveDocumentIds = state.selectedDocumentIds.map { it.value }
                        libraryViewModel.clearSelection()
                        isViewingVault = true
                    }
                },
                onOpenSubRoute = { activeSubRoute = it },
                libraryViewModel = libraryViewModel,
                appLocaleController = appLocaleController,
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        AppDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { selectDestination(item) },
                                icon = { Icon(if (destination == item) item.selectedIcon else item.icon, null) },
                                label = { Text(item.label()) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryMasterPane(
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    libraryViewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            HomeLibraryScreen(
                state = state,
                fileStore = fileStore,
                gridState = gridState,
                listState = listState,
                onOpenDocument = libraryViewModel::openDocument,
                onSearchQueryChange = libraryViewModel::setSearchQuery,
                onFilterChange = libraryViewModel::setFilter,
                onToggleSelectionMode = libraryViewModel::toggleSelectionMode,
                onToggleDocumentSelection = libraryViewModel::toggleDocumentSelection,
                onOpenFolder = libraryViewModel::openFolder,
                onCreateFolderRequested = {},
                emptyContent = {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.library_no_documents), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun OScanNavigationRail(selected: AppDestination, onDestinationSelected: (AppDestination) -> Unit) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        Spacer(Modifier.height(12.dp))
        AppDestination.entries.forEach { item ->
            NavigationRailItem(
                selected = selected == item,
                onClick = { onDestinationSelected(item) },
                icon = { Icon(if (selected == item) item.selectedIcon else item.icon, null) },
                label = { Text(item.label()) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DestinationScaffold(
    destination: AppDestination,
    pagerState: PagerState,
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snackbarHostState: SnackbarHostState,
    onDestinationSelected: (AppDestination) -> Unit,
    onImportImages: () -> Unit,
    cameraViewModel: CameraViewModel? = null,
    captureState: CameraCaptureState? = null,
    onCaptured: ((File) -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    onOpenFolders: () -> Unit,
    onOpenVault: (() -> Unit)? = null,
    onMoveSelectedToVault: () -> Unit = {},
    onOpenSubRoute: (SettingsSubRoute) -> Unit,
    libraryViewModel: LibraryViewModel,
    appLocaleController: AppLocaleController? = null,
    modifier: Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    var bulkMoveDialogOpen by remember { mutableStateOf(false) }
    var bulkTrashConfirmOpen by remember { mutableStateOf(false) }
    var createFolderDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (destination != AppDestination.Scan && destination != AppDestination.Me) {
                if (state.selectionMode && destination == AppDestination.Home) {
                    TopAppBar(
                        title = {
                            Text(
                            stringResource(R.string.selection_count, state.selectedDocumentIds.size),
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = libraryViewModel::clearSelection) {
                                Icon(Icons.Default.Close, "Cancel selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = onMoveSelectedToVault) {
                                Icon(Icons.Default.Lock, "Move selected to Vault")
                            }
                            IconButton(onClick = { libraryViewModel.bulkSetFavorite(true) }) {
                                Icon(Icons.Default.Favorite, "Favorite selected")
                            }
                            IconButton(onClick = { bulkMoveDialogOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, "Move selected")
                            }
                            IconButton(onClick = { bulkTrashConfirmOpen = true }) {
                                Icon(Icons.Default.Delete, "Trash selected")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(if (destination == AppDestination.Home) "OScan" else destination.label()) },
                        actions = {
                            if (destination == AppDestination.Home) {
                                IconButton(onClick = onOpenFolders) {
                                    Icon(Icons.Default.Folder, "Folders")
                                }
                                IconButton(onClick = libraryViewModel::openTrash) {
                                    Icon(Icons.Default.Delete, "Trash")
                                }
                                if (state.documents.isNotEmpty()) {
                                    Box {
                                        IconButton(onClick = { sortMenuOpen = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort documents") }
                                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                            DocumentSort.entries.forEach { sort ->
                                                DropdownMenuItem(
                                                    text = { Text(sort.label()) },
                                                    onClick = { sortMenuOpen = false; libraryViewModel.setSort(sort) },
                                                    trailingIcon = { if (sort == state.sort) Text("✓") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            beyondBoundsPageCount = 1,
            userScrollEnabled = !sortMenuOpen && !bulkMoveDialogOpen &&
                !bulkTrashConfirmOpen && !createFolderDialogOpen
        ) { pageIndex ->
            val pageDestination = AppDestination.entries[pageIndex]
            val cameraIsSettled = pagerState.settledPage == AppDestination.Scan.ordinal &&
                !pagerState.isScrollInProgress
            Box(Modifier.fillMaxSize()) {
            when (pageDestination) {
                AppDestination.Home -> HomeLibraryScreen(
                    state = state,
                    fileStore = fileStore,
                    gridState = gridState,
                    listState = listState,
                    onOpenDocument = libraryViewModel::openDocument,
                    onSearchQueryChange = libraryViewModel::setSearchQuery,
                    onFilterChange = libraryViewModel::setFilter,
                    onToggleSelectionMode = libraryViewModel::toggleSelectionMode,
                    onToggleDocumentSelection = libraryViewModel::toggleDocumentSelection,
                    onOpenFolder = libraryViewModel::openFolder,
                    onCreateFolderRequested = { createFolderDialogOpen = true },
                    onOpenVault = onOpenVault
                ) { EmptyHomeScreen { onDestinationSelected(AppDestination.Scan) } }
                AppDestination.Scan -> {
                    if (cameraViewModel != null && captureState != null && onCaptured != null && onDone != null) {
                        BackHandler(enabled = cameraIsSettled) {
                            if (captureState.capturedCount > 0) {
                                onDone()
                            } else {
                                onDestinationSelected(AppDestination.Home)
                            }
                        }
                        if (cameraIsSettled) {
                            LiveCameraScreen(
                                cameraViewModel = cameraViewModel,
                                captureState = captureState,
                                onCaptured = onCaptured,
                                onDone = onDone,
                                onImport = onImportImages,
                                shutterFeedbackEnabled = state.userPreferences.shutterFeedback
                            )
                        } else {
                            CameraTransitionPreview()
                        }
                    } else {
                        ScanEntryScreen(
                            onOpenCamera = { onDestinationSelected(AppDestination.Scan) },
                            onImportImages = onImportImages
                        )
                    }
                }
                AppDestination.Me -> MeScreen(
                    userPreferences = state.userPreferences,
                    appLocaleController = appLocaleController,
                    trashCount = state.trashDocuments.size,
                    foldersCount = state.folders.size,
                    onOpenFolders = onOpenFolders,
                    onOpenTrash = libraryViewModel::openTrash,
                    onOpenSubRoute = onOpenSubRoute,
                    onUpdateDisplayName = libraryViewModel::setDisplayName,
                    onUpdateAvatarPreset = libraryViewModel::setAvatarPreset
                )
            }
            }
        }
    }

    if (bulkMoveDialogOpen) {
        MoveDialog(
            currentFolderId = null,
            folders = state.folders,
            onDismiss = { bulkMoveDialogOpen = false },
            onConfirm = { folderId ->
                libraryViewModel.bulkMoveToFolder(folderId)
                bulkMoveDialogOpen = false
            }
        )
    }

    if (bulkTrashConfirmOpen) {
        val count = state.selectedDocumentIds.size
        AlertDialog(
            onDismissRequest = { bulkTrashConfirmOpen = false },
            title = { Text(stringResource(R.string.dialog_move_selected_trash_title)) },
            text = { Text(stringResource(R.string.dialog_move_selected_trash_body)) },
            confirmButton = {
                TextButton(onClick = {
                    libraryViewModel.bulkMoveToTrash()
                    bulkTrashConfirmOpen = false
                }) {
                    Text(stringResource(R.string.action_move_to_trash))
                }
            },
            dismissButton = { TextButton(onClick = { bulkTrashConfirmOpen = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (createFolderDialogOpen) {
        CreateFolderDialog(
            onDismiss = { createFolderDialogOpen = false },
            onConfirm = { name ->
                libraryViewModel.createFolder(name)
                createFolderDialogOpen = false
            }
        )
    }
}

private enum class SettingsSubRoute {
    NONE, CAPTURE, ENHANCEMENT, EXPORT, APPEARANCE, LANGUAGE, STORAGE, PRIVACY, HELP, ABOUT
}

@Composable
private fun DocumentSort.label(): String = when (this) {
    DocumentSort.MODIFIED_DESC -> stringResource(R.string.library_sort_date_desc)
    DocumentSort.MODIFIED_ASC -> stringResource(R.string.library_sort_date_asc)
    DocumentSort.CREATED_DESC -> stringResource(R.string.library_sort_created_desc)
    DocumentSort.CREATED_ASC -> stringResource(R.string.library_sort_created_asc)
    DocumentSort.NAME_ASC -> stringResource(R.string.library_sort_name_asc)
    DocumentSort.NAME_DESC -> stringResource(R.string.library_sort_name_desc)
}

@Composable
private fun EmptyHomeScreen(onScanDocument: () -> Unit) {
    EmptyStateLayout(
        icon = Icons.Default.Description,
        title = stringResource(R.string.library_home_empty_title),
        supportingText = stringResource(R.string.library_home_empty_body)
    ) {
        Button(onClick = onScanDocument, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoCamera, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_scan_document))
        }
    }
}

@Composable
private fun ScanEntryScreen(onOpenCamera: () -> Unit, onImportImages: () -> Unit) {
    EmptyStateLayout(
        icon = Icons.Default.PhotoCamera,
        title = stringResource(R.string.scan_entry_title),
        supportingText = stringResource(R.string.scan_entry_body)
    ) {
        Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoCamera, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_open_camera))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onImportImages, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_import_images))
        }
    }
}

@Composable
private fun MeScreen(
    userPreferences: com.oscan.android.data.preferences.UserPreferences,
    appLocaleController: AppLocaleController?,
    trashCount: Int,
    foldersCount: Int,
    onOpenFolders: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSubRoute: (SettingsSubRoute) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateAvatarPreset: (String) -> Unit
) {
    var editNameDialogOpen by remember { mutableStateOf(false) }
    var editAvatarDialogOpen by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember(context) { com.oscan.android.util.AppVersionUtils.getVersionName(context) }

    val avatarBgColor = when (userPreferences.avatarPreset) {
        "INDIGO" -> androidx.compose.ui.graphics.Color(0xFF3F51B5)
        "AMBER" -> androidx.compose.ui.graphics.Color(0xFFFFB300)
        "GREEN" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "PURPLE" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val avatarFgColor = when (userPreferences.avatarPreset) {
        "TEAL", "DEFAULT" -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> androidx.compose.ui.graphics.Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                color = avatarBgColor,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .size(80.dp)
                    .clickable { editAvatarDialogOpen = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.cd_avatar),
                        tint = avatarFgColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(userPreferences.displayName, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { editNameDialogOpen = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_edit_name),
                    modifier = Modifier.size(18.dp)
                )
            }
        }


        Spacer(Modifier.height(20.dp))

        // Quick Navigation Section
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFolders)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.folders_overview), style = MaterialTheme.typography.titleMedium)
                    Text(pluralStringResource(R.plurals.plural_folders, foldersCount, foldersCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTrash)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.library_tab_trash), style = MaterialTheme.typography.titleMedium)
                    Text(if (trashCount == 1) "1 item in Trash" else "$trashCount items in Trash", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Settings Section
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            MeSettingRow(stringResource(R.string.settings_capture_row), stringResource(R.string.settings_capture_row_desc), Icons.Default.CameraAlt) { onOpenSubRoute(SettingsSubRoute.CAPTURE) }
            MeSettingRow(stringResource(R.string.settings_enhancement_row), stringResource(R.string.settings_enhancement_row_desc), Icons.Default.AutoAwesome) { onOpenSubRoute(SettingsSubRoute.ENHANCEMENT) }
            MeSettingRow(stringResource(R.string.settings_export_row), stringResource(R.string.settings_export_row_desc), Icons.Default.PictureAsPdf) { onOpenSubRoute(SettingsSubRoute.EXPORT) }
            MeSettingRow(stringResource(R.string.settings_appearance_row), stringResource(R.string.settings_appearance_row_desc), Icons.Default.Palette) { onOpenSubRoute(SettingsSubRoute.APPEARANCE) }
            val currentLang = appLocaleController?.currentLanguage() ?: AppLanguage.SYSTEM
            val langSubtitle = when (currentLang) {
                AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                AppLanguage.SIMPLIFIED_CHINESE -> stringResource(R.string.language_simplified_chinese)
                AppLanguage.JAPANESE -> stringResource(R.string.language_japanese)
            }
            MeSettingRow(
                title = stringResource(R.string.settings_language_title),
                subtitle = langSubtitle,
                icon = Icons.Default.Language
            ) { onOpenSubRoute(SettingsSubRoute.LANGUAGE) }
            MeSettingRow(stringResource(R.string.settings_storage_title), stringResource(R.string.settings_storage_row_desc), Icons.Default.Storage) { onOpenSubRoute(SettingsSubRoute.STORAGE) }
        }

        Spacer(Modifier.height(24.dp))

        // Product Info Section
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.product_information), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            MeSettingRow(stringResource(R.string.privacy_title), stringResource(R.string.settings_privacy_row_desc), Icons.Default.PrivacyTip) { onOpenSubRoute(SettingsSubRoute.PRIVACY) }
            MeSettingRow(stringResource(R.string.help_title), stringResource(R.string.settings_help_row_desc), Icons.Default.HelpOutline) { onOpenSubRoute(SettingsSubRoute.HELP) }
            MeSettingRow(stringResource(R.string.about_title), stringResource(R.string.settings_about_row_desc, versionName), Icons.Default.Info) { onOpenSubRoute(SettingsSubRoute.ABOUT) }
        }
    }

    if (editNameDialogOpen) {
        var tempName by remember { mutableStateOf(userPreferences.displayName) }
        AlertDialog(
            onDismissRequest = { editNameDialogOpen = false },
            title = { Text(stringResource(R.string.profile_edit_name)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.profile_display_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) onUpdateDisplayName(tempName.trim())
                    editNameDialogOpen = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = { editNameDialogOpen = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (editAvatarDialogOpen) {
        AlertDialog(
            onDismissRequest = { editAvatarDialogOpen = false },
            title = { Text(stringResource(R.string.profile_avatar_color)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "TEAL" to stringResource(R.string.avatar_teal),
                        "INDIGO" to stringResource(R.string.avatar_indigo),
                        "AMBER" to stringResource(R.string.avatar_amber),
                        "GREEN" to stringResource(R.string.avatar_green),
                        "PURPLE" to stringResource(R.string.avatar_purple)
                    ).forEach { (presetKey, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateAvatarPreset(presetKey)
                                    editAvatarDialogOpen = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userPreferences.avatarPreset == presetKey,
                                onClick = {
                                    onUpdateAvatarPreset(presetKey)
                                    editAvatarDialogOpen = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editAvatarDialogOpen = false }) { Text(stringResource(R.string.action_close)) } }
        )
    }
}

@Composable
private fun MeSettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

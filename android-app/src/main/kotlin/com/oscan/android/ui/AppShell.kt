package com.oscan.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.preferences.DocumentSort
import com.oscan.android.data.preferences.LibraryPresentation
import com.oscan.android.data.storage.DocumentFileStore

private enum class AppDestination(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Scan("Scan", Icons.Filled.Scanner, Icons.Outlined.Scanner),
    Me("Me", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun OScanAppShell(
    libraryViewModel: LibraryViewModel,
    fileStore: DocumentFileStore,
    onImportImages: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val state by libraryViewModel.uiState.collectAsState()
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var viewerPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var isViewingFolders by rememberSaveable { mutableStateOf(false) }
    var createFolderDialogOpen by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            libraryViewModel.clearMessage()
        }
    }

    if (viewerPage != null && state.selectedDocument != null) {
        BackHandler { viewerPage = null }
        PageViewerScreen(state.selectedDocument!!, viewerPage!!, fileStore) { viewerPage = null }
        return
    }

    if (state.selectedDocumentId != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        BackHandler { libraryViewModel.closeDocument() }
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
            onSavePdf = { ctx, doc, uri -> libraryViewModel.exportDocumentPdfToUri(ctx, doc, fileStore, uri) },
            onSharePdf = { ctx, doc -> libraryViewModel.shareDocumentPdf(ctx, doc, fileStore, context::startActivity) },
            onOpenPage = { viewerPage = it }
        )
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
                OScanNavigationRail(destination) { destination = it }
                DestinationScaffold(
                    destination = destination,
                    state = state,
                    fileStore = fileStore,
                    gridState = gridState,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    onDestinationSelected = { destination = it },
                    onImportImages = onImportImages,
                    onOpenCamera = onOpenCamera,
                    onOpenFolders = { isViewingFolders = true },
                    libraryViewModel = libraryViewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DestinationScaffold(
                destination = destination,
                state = state,
                fileStore = fileStore,
                gridState = gridState,
                listState = listState,
                snackbarHostState = snackbarHostState,
                onDestinationSelected = { destination = it },
                onImportImages = onImportImages,
                onOpenCamera = onOpenCamera,
                onOpenFolders = { isViewingFolders = true },
                libraryViewModel = libraryViewModel,
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        AppDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Icon(if (destination == item) item.selectedIcon else item.icon, null) },
                                label = { Text(item.label) }
                            )
                        }
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
                label = { Text(item.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationScaffold(
    destination: AppDestination,
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snackbarHostState: SnackbarHostState,
    onDestinationSelected: (AppDestination) -> Unit,
    onImportImages: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFolders: () -> Unit,
    libraryViewModel: LibraryViewModel,
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
            if (state.selectionMode && destination == AppDestination.Home) {
                TopAppBar(
                    title = { Text("${state.selectedDocumentIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = libraryViewModel::clearSelection) {
                            Icon(Icons.Default.Close, "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { libraryViewModel.selectAll(state.documents.map { it.id }) }) {
                            Icon(Icons.Default.SelectAll, "Select all")
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
                    title = { Text(if (destination == AppDestination.Home) "OScan" else destination.label) },
                    actions = {
                        if (destination == AppDestination.Home) {
                            IconButton(onClick = onOpenFolders) {
                                Icon(Icons.Default.Folder, "Folders")
                            }
                            IconButton(onClick = libraryViewModel::openTrash) {
                                Icon(Icons.Default.Delete, "Trash")
                            }
                            if (state.documents.isNotEmpty()) {
                                IconButton(onClick = {
                                    libraryViewModel.setPresentation(
                                        if (state.presentation == LibraryPresentation.GRID) LibraryPresentation.LIST else LibraryPresentation.GRID
                                    )
                                }) {
                                    Icon(
                                        if (state.presentation == LibraryPresentation.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        if (state.presentation == LibraryPresentation.GRID) "Show list" else "Show grid"
                                    )
                                }
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
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
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
                    onCreateFolderRequested = { createFolderDialogOpen = true }
                ) { EmptyHomeScreen({ onDestinationSelected(AppDestination.Scan) }, onImportImages) }
                AppDestination.Scan -> ScanEntryScreen(onOpenCamera, onImportImages)
                AppDestination.Me -> MeScreen(
                    trashCount = state.trashDocuments.size,
                    foldersCount = state.folders.size,
                    onOpenFolders = onOpenFolders,
                    onOpenTrash = libraryViewModel::openTrash
                )
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
            title = { Text("Move $count document(s) to Trash?") },
            text = { Text("Selected documents will be moved to local Trash.") },
            confirmButton = {
                TextButton(onClick = {
                    libraryViewModel.bulkMoveToTrash()
                    bulkTrashConfirmOpen = false
                }) {
                    Text("Move to Trash")
                }
            },
            dismissButton = { TextButton(onClick = { bulkTrashConfirmOpen = false }) { Text("Cancel") } }
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

private fun DocumentSort.label(): String = when (this) {
    DocumentSort.MODIFIED_DESC -> "Modified: newest"
    DocumentSort.MODIFIED_ASC -> "Modified: oldest"
    DocumentSort.CREATED_DESC -> "Created: newest"
    DocumentSort.CREATED_ASC -> "Created: oldest"
    DocumentSort.NAME_ASC -> "Name: A–Z"
    DocumentSort.NAME_DESC -> "Name: Z–A"
}

@Composable
private fun EmptyHomeScreen(onScanDocument: () -> Unit, onImportImages: () -> Unit) {
    EmptyStateLayout(
        icon = Icons.Default.Description,
        title = "Your documents will appear here",
        supportingText = "Scan with the camera or import images. Everything stays on this device."
    ) {
        Button(onClick = onScanDocument, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Scanner, null)
            Spacer(Modifier.width(8.dp))
            Text("Scan document")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onImportImages, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text("Import images")
        }
    }
}

@Composable
private fun ScanEntryScreen(onOpenCamera: () -> Unit, onImportImages: () -> Unit) {
    EmptyStateLayout(
        icon = Icons.Default.Scanner,
        title = "Scan a document",
        supportingText = "Capture pages with the camera or import images. Everything is processed and saved locally."
    ) {
        Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Scanner, null)
            Spacer(Modifier.width(8.dp))
            Text("Open camera")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onImportImages, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text("Import images")
        }
    }
}

@Composable
private fun MeScreen(
    trashCount: Int,
    foldersCount: Int,
    onOpenFolders: () -> Unit,
    onOpenTrash: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(28.dp), modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your local workspace", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("No account needed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Folders row
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFolders)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Folders overview", style = MaterialTheme.typography.titleMedium)
                    Text("$foldersCount folder(s)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Trash row
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTrash)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Trash", style = MaterialTheme.typography.titleMedium)
                    Text(if (trashCount == 1) "1 item in Trash" else "$trashCount items in Trash", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Private by design", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Documents and image processing stay on this device. Sharing is always an explicit action.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

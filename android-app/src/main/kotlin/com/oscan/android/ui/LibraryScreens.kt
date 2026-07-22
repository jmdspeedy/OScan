package com.oscan.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.model.Page
import com.oscan.android.data.model.PageId
import com.oscan.android.data.preferences.LibraryPresentation
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.ui.theme.OScanTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeLibraryScreen(
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    gridState: LazyGridState,
    listState: LazyListState,
    onOpenDocument: (DocumentId) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (DocumentFilter) -> Unit,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit,
    onOpenFolder: (FolderId) -> Unit,
    onCreateFolderRequested: () -> Unit,
    emptyContent: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Search bar
        PaddingValues(horizontal = 16.dp, vertical = 8.dp).let { padding ->
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                placeholder = { Text("Search names") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Filter chips row
        FilterChipsRow(
            currentFilter = state.filter,
            onFilterChange = onFilterChange,
            foldersCount = state.folders.size,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.documents.isEmpty() && state.searchQuery.isNotEmpty() -> NoSearchResultsState(state.searchQuery)
            state.documents.isEmpty() -> emptyContent()
            else -> DocumentList(
                state = state,
                fileStore = fileStore,
                listState = listState,
                onOpenDocument = onOpenDocument,
                onToggleSelectionMode = onToggleSelectionMode,
                onToggleDocumentSelection = onToggleDocumentSelection,
                onOpenFolder = onOpenFolder
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    currentFilter: DocumentFilter,
    onFilterChange: (DocumentFilter) -> Unit,
    foldersCount: Int,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = currentFilter == DocumentFilter.ALL,
                onClick = { onFilterChange(DocumentFilter.ALL) },
                label = { Text("All") }
            )
        }
        item {
            FilterChip(
                selected = currentFilter == DocumentFilter.FAVORITES,
                onClick = { onFilterChange(DocumentFilter.FAVORITES) },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = currentFilter == DocumentFilter.UNFILED,
                onClick = { onFilterChange(DocumentFilter.UNFILED) },
                label = { Text("Unfiled") }
            )
        }
    }
}

@Composable
private fun NoSearchResultsState(query: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("No matching documents", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("No documents or folders match \"$query\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DocumentGrid(
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    gridState: LazyGridState,
    onOpenDocument: (DocumentId) -> Unit,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit,
    onOpenFolder: (FolderId) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.searchQuery.isEmpty() && state.recentDocuments.isNotEmpty() && !state.selectionMode) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Recent") }
            items(state.recentDocuments, key = { "recent-${it.id.value}" }) { document ->
                DocumentCard(
                    document = document,
                    fileStore = fileStore,
                    selectionMode = false,
                    isSelected = false,
                    onOpenDocument = onOpenDocument,
                    onToggleSelectionMode = onToggleSelectionMode,
                    onToggleDocumentSelection = onToggleDocumentSelection
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("All documents", Modifier.padding(top = 12.dp))
            }
        }
        items(state.documents, key = { "all-${it.id.value}" }) { document ->
            val isSelected = state.selectedDocumentIds.contains(document.id)
            DocumentCard(
                document = document,
                fileStore = fileStore,
                selectionMode = state.selectionMode,
                isSelected = isSelected,
                onOpenDocument = onOpenDocument,
                onToggleSelectionMode = onToggleSelectionMode,
                onToggleDocumentSelection = onToggleDocumentSelection
            )
        }
    }
}

@Composable
private fun DocumentList(
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    listState: LazyListState,
    onOpenDocument: (DocumentId) -> Unit,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit,
    onOpenFolder: (FolderId) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (state.searchQuery.isEmpty() && state.recentDocuments.isNotEmpty() && !state.selectionMode) {
            item { SectionTitle("Recent", Modifier.padding(horizontal = 16.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recentDocuments, key = { it.id.value }) { document ->
                        Box(Modifier.width(156.dp)) {
                            DocumentCard(
                                document = document,
                                fileStore = fileStore,
                                selectionMode = false,
                                isSelected = false,
                                onOpenDocument = onOpenDocument,
                                onToggleSelectionMode = onToggleSelectionMode,
                                onToggleDocumentSelection = onToggleDocumentSelection
                            )
                        }
                    }
                }
            }
            item { SectionTitle("All documents", Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp)) }
        }
        items(state.documents, key = { it.id.value }) { document ->
            val isSelected = state.selectedDocumentIds.contains(document.id)
            DocumentRow(
                document = document,
                fileStore = fileStore,
                selectionMode = state.selectionMode,
                isSelected = isSelected,
                onOpenDocument = onOpenDocument,
                onToggleSelectionMode = onToggleSelectionMode,
                onToggleDocumentSelection = onToggleDocumentSelection
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(bottom = 12.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentCard(
    document: Document,
    fileStore: DocumentFileStore,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDocument: (DocumentId) -> Unit,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleDocumentSelection(document.id)
                    else onOpenDocument(document.id)
                },
                onLongClick = {
                    onToggleSelectionMode(document.id)
                }
            )
    ) {
        Box {
            Column {
                Thumbnail(
                    path = document.pages.firstOrNull()?.thumbnailAsset,
                    fileStore = fileStore,
                    description = "${document.name} thumbnail",
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                )
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            document.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (document.isFavorite) {
                            Icon(Icons.Filled.Favorite, "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(document.metadataLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    document.folder?.let {
                        Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleDocumentSelection(document.id) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    document: Document,
    fileStore: DocumentFileStore,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDocument: (DocumentId) -> Unit,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleDocumentSelection(document.id)
                    else onOpenDocument(document.id)
                },
                onLongClick = { onToggleSelectionMode(document.id) }
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleDocumentSelection(document.id) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Thumbnail(
            path = document.pages.firstOrNull()?.thumbnailAsset,
            fileStore = fileStore,
            description = "${document.name} thumbnail",
            modifier = Modifier.size(width = 72.dp, height = 96.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(document.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(document.metadataLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            document.folder?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (document.isFavorite) Icon(Icons.Filled.Favorite, "Favorite", tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderOverviewScreen(
    folders: List<Folder>,
    documents: List<Document>,
    onOpenFolder: (FolderId) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (FolderId, String) -> Unit,
    onDeleteFolder: (FolderId) -> Unit,
    onBack: () -> Unit
) {
    var createDialogOpen by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { createDialogOpen = true }) {
                Icon(Icons.Default.CreateNewFolder, "New Folder")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (folders.isEmpty()) {
                EmptyStateLayout(
                    icon = Icons.Default.Folder,
                    title = "No folders yet",
                    supportingText = "Organize your documents by creating folders."
                ) {
                    Button(onClick = { createDialogOpen = true }) {
                        Icon(Icons.Default.CreateNewFolder, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Folder")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(folders, key = { it.id.value }) { folder ->
                        val count = documents.count { it.folder?.id == folder.id }
                        FolderCard(
                            folder = folder,
                            documentCount = count,
                            onClick = { onOpenFolder(folder.id) },
                            onRename = { folderToRename = folder },
                            onDelete = { folderToDelete = folder }
                        )
                    }
                }
            }
        }
    }

    if (createDialogOpen) CreateFolderDialog({ createDialogOpen = false }) { onCreateFolder(it); createDialogOpen = false }
    folderToRename?.let { folder ->
        RenameFolderDialog(folder.name, { folderToRename = null }) { onRenameFolder(folder.id, it); folderToRename = null }
    }
    folderToDelete?.let { folder ->
        val count = documents.count { it.folder?.id == folder.id }
        DeleteFolderDialog(folder.name, count, { folderToDelete = null }) { onDeleteFolder(folder.id); folderToDelete = null }
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    documentCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Folder options") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete folder") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(if (documentCount == 1) "1 document" else "$documentCount documents", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: Folder,
    documents: List<Document>,
    fileStore: DocumentFileStore,
    gridState: LazyGridState,
    listState: LazyListState,
    presentation: LibraryPresentation,
    selectionMode: Boolean,
    selectedDocumentIds: Set<DocumentId>,
    onOpenDocument: (DocumentId) -> Unit,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onBack: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }

    val folderDocs = documents.filter { it.folder?.id == folder.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Folder actions") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Rename folder") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menuOpen = false; renameOpen = true })
                            DropdownMenuItem(text = { Text("Delete folder") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuOpen = false; deleteOpen = true })
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (folderDocs.isEmpty()) {
                EmptyStateLayout(
                    icon = Icons.Default.Folder,
                    title = "This folder is empty",
                    supportingText = "Move existing documents here or scan into this folder."
                ) {
                    Button(onClick = onBack) { Text("Back to Folders") }
                }
            } else if (presentation == LibraryPresentation.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(156.dp),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(folderDocs, key = { it.id.value }) { doc ->
                        DocumentCard(
                            document = doc,
                            fileStore = fileStore,
                            selectionMode = selectionMode,
                            isSelected = selectedDocumentIds.contains(doc.id),
                            onOpenDocument = onOpenDocument,
                            onToggleSelectionMode = onToggleSelectionMode,
                            onToggleDocumentSelection = onToggleDocumentSelection
                        )
                    }
                }
            } else {
                LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(folderDocs, key = { it.id.value }) { doc ->
                        DocumentRow(
                            document = doc,
                            fileStore = fileStore,
                            selectionMode = selectionMode,
                            isSelected = selectedDocumentIds.contains(doc.id),
                            onOpenDocument = onOpenDocument,
                            onToggleSelectionMode = onToggleSelectionMode,
                            onToggleDocumentSelection = onToggleDocumentSelection
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (renameOpen) RenameFolderDialog(folder.name, { renameOpen = false }) { onRenameFolder(it); renameOpen = false }
    if (deleteOpen) DeleteFolderDialog(folder.name, folderDocs.size, { deleteOpen = false }) { onDeleteFolder(); deleteOpen = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashDocuments: List<Document>,
    fileStore: DocumentFileStore,
    selectionMode: Boolean,
    selectedDocumentIds: Set<DocumentId>,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit,
    onRestoreDocument: (DocumentId) -> Unit,
    onPermanentlyDeleteDocument: (DocumentId) -> Unit,
    onRestoreMultiple: (List<DocumentId>) -> Unit,
    onPermanentlyDeleteMultiple: (List<DocumentId>) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    var emptyTrashDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash (${trashDocuments.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (trashDocuments.isNotEmpty()) {
                        TextButton(onClick = { emptyTrashDialogOpen = true }) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (trashDocuments.isEmpty()) {
                EmptyStateLayout(
                    icon = Icons.Default.Delete,
                    title = "Trash is empty",
                    supportingText = "Documents moved to Trash will appear here until permanently deleted."
                ) {
                    Button(onClick = onBack) { Text("Back to Home") }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(trashDocuments, key = { it.id.value }) { document ->
                        TrashDocumentRow(
                            document = document,
                            fileStore = fileStore,
                            selectionMode = selectionMode,
                            isSelected = selectedDocumentIds.contains(document.id),
                            onToggleSelectionMode = onToggleSelectionMode,
                            onToggleDocumentSelection = onToggleDocumentSelection,
                            onRestore = { onRestoreDocument(document.id) },
                            onDelete = { onPermanentlyDeleteDocument(document.id) }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (emptyTrashDialogOpen) {
        AlertDialog(
            onDismissRequest = { emptyTrashDialogOpen = false },
            title = { Text("Empty Trash?") },
            text = { Text("All ${trashDocuments.size} document(s) in Trash will be permanently deleted from this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { emptyTrashDialogOpen = false; onEmptyTrash() }) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { emptyTrashDialogOpen = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashDocumentRow(
    document: Document,
    fileStore: DocumentFileStore,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelectionMode: (DocumentId?) -> Unit,
    onToggleDocumentSelection: (DocumentId) -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleDocumentSelection(document.id)
                },
                onLongClick = { onToggleSelectionMode(document.id) }
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleDocumentSelection(document.id) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Thumbnail(
            path = document.pages.firstOrNull()?.thumbnailAsset,
            fileStore = fileStore,
            description = "${document.name} thumbnail",
            modifier = Modifier.size(width = 72.dp, height = 96.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(document.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(document.metadataLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = onRestore) { Icon(Icons.Default.Restore, "Restore document", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteForever, "Permanently delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = { TextField(name, { name = it }, label = { Text("Folder name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameFolderDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename folder") },
        text = { TextField(name, { name = it }, label = { Text("Folder name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank() && name.trim() != currentName) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteFolderDialog(folderName: String, documentCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete folder \"$folderName\"?") },
        text = { Text("Deleting this folder will move its $documentCount document(s) to Unfiled. The documents will not be deleted.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete Folder", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: Document?,
    requestedDocumentExists: Boolean,
    folders: List<Folder>,
    fileStore: DocumentFileStore,
    snackbarHostState: SnackbarHostState,
    isExporting: Boolean = false,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onFavorite: (Boolean) -> Unit,
    onMove: (FolderId?) -> Unit,
    onTrash: () -> Unit,
    onSavePdf: (Context, Document, Uri) -> Unit,
    onSharePdf: (Context, Document) -> Unit,
    onOpenPage: (Int) -> Unit,
    onReorderPages: (List<PageId>) -> Unit = {},
    onRotatePage: (PageId, Int) -> Unit = { _, _ -> },
    onDeletePage: (PageId) -> Unit = {},
    onAddPages: () -> Unit = {},
    onEditPage: (Page) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val compactActions = configuration.screenWidthDp < 600 || configuration.fontScale >= 1.5f
    var overflowOpen by remember { mutableStateOf(false) }
    var renameOpen by rememberSaveable { mutableStateOf(false) }
    var moveOpen by rememberSaveable { mutableStateOf(false) }
    var trashOpen by rememberSaveable { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<Page?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && document != null) {
            onSavePdf(context, document, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (document != null) {
                        if (!compactActions) {
                            IconButton(onClick = onAddPages) {
                                Icon(Icons.Default.AddPhotoAlternate, "Add pages")
                            }
                            IconButton(
                                onClick = { onSharePdf(context, document) },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Default.Share, "Share PDF")
                            }
                            IconButton(
                                onClick = { createDocumentLauncher.launch("${document.name}.pdf") },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Default.PictureAsPdf, "Save PDF")
                            }
                        }
                        IconButton(onClick = { onFavorite(!document.isFavorite) }) {
                            Icon(if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (document.isFavorite) "Remove favorite" else "Add favorite")
                        }
                        Box {
                            IconButton(onClick = { overflowOpen = true }) { Icon(Icons.Default.MoreVert, "Document actions") }
                            DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Add pages") },
                                    leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, null) },
                                    onClick = { overflowOpen = false; onAddPages() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                    onClick = { overflowOpen = false; createDocumentLauncher.launch("${document.name}.pdf") },
                                    enabled = !isExporting
                                )
                                DropdownMenuItem(
                                    text = { Text("Share PDF") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = { overflowOpen = false; onSharePdf(context, document) },
                                    enabled = !isExporting
                                )
                                DropdownMenuItem(
                                    text = { Text(if (document.isFavorite) "Remove favorite" else "Add favorite") },
                                    leadingIcon = { Icon(if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null) },
                                    onClick = { overflowOpen = false; onFavorite(!document.isFavorite) }
                                )
                                if (folders.isNotEmpty() || document.folder != null) {
                                    DropdownMenuItem(text = { Text("Move to folder") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) }, onClick = { overflowOpen = false; moveOpen = true })
                                }
                                DropdownMenuItem(text = { Text("Move to Trash") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { overflowOpen = false; trashOpen = true })
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !requestedDocumentExists -> MissingDocument(Modifier, onBack)
                document == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = document.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(onClickLabel = "Rename document") { renameOpen = true }
                                    .padding(vertical = 4.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(document.metadataLine(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            document.folder?.let { Text("Folder: ${it.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pages", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = onAddPages) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add page")
                                }
                            }
                        }
                    }
                    items(document.pages, key = { it.id.value }) { page ->
                        val position = page.position
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Box {
                                    Thumbnail(
                                        path = page.thumbnailAsset,
                                        fileStore = fileStore,
                                        description = "Page ${position + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(page.safeAspectRatio())
                                            .clickable { onOpenPage(position) },
                                        rotationDegrees = page.rotationDegrees
                                    )
                                    var pageMenuOpen by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { pageMenuOpen = true },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                    ) {
                                        Icon(Icons.Default.MoreVert, "Page options")
                                    }
                                    DropdownMenu(expanded = pageMenuOpen, onDismissRequest = { pageMenuOpen = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Rotate left") },
                                            leadingIcon = { Icon(Icons.Default.RotateLeft, null) },
                                            onClick = { pageMenuOpen = false; onRotatePage(page.id, -90) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rotate right") },
                                            leadingIcon = { Icon(Icons.Default.RotateRight, null) },
                                            onClick = { pageMenuOpen = false; onRotatePage(page.id, 90) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Re-crop & treatment") },
                                            leadingIcon = { Icon(Icons.Default.Crop, null) },
                                            onClick = { pageMenuOpen = false; onEditPage(page) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Remove page") },
                                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                                            onClick = { pageMenuOpen = false; pageToDelete = page }
                                        )
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Page ${position + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val pageIds = document.pages.map { it.id }.toMutableList()
                                            val idx = position
                                            if (idx > 0) {
                                                val tmp = pageIds[idx]
                                                pageIds[idx] = pageIds[idx - 1]
                                                pageIds[idx - 1] = tmp
                                                onReorderPages(pageIds)
                                            }
                                        },
                                        enabled = position > 0,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, "Move page ${position + 1} left")
                                    }
                                    IconButton(
                                        onClick = {
                                            val pageIds = document.pages.map { it.id }.toMutableList()
                                            val idx = position
                                            if (idx < document.pages.lastIndex) {
                                                val tmp = pageIds[idx]
                                                pageIds[idx] = pageIds[idx + 1]
                                                pageIds[idx + 1] = tmp
                                                onReorderPages(pageIds)
                                            }
                                        },
                                        enabled = position < document.pages.lastIndex,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateNext, "Move page ${position + 1} right")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isExporting) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(16.dp))
                        Text("Generating PDF…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (renameOpen && document != null) RenameDialog(document.name, { renameOpen = false }) { onRename(it); renameOpen = false }
    if (moveOpen && document != null) MoveDialog(document.folder?.id, folders, { moveOpen = false }) { onMove(it); moveOpen = false }
    if (trashOpen) AlertDialog(
        onDismissRequest = { trashOpen = false },
        title = { Text("Move document to Trash?") },
        text = { Text("It will be removed from Home and kept in local Trash.") },
        confirmButton = { TextButton(onClick = { trashOpen = false; onTrash() }) { Text("Move to Trash") } },
        dismissButton = { TextButton(onClick = { trashOpen = false }) { Text("Cancel") } }
    )

    pageToDelete?.let { targetPage ->
        val isFinalPage = document != null && document.pages.size == 1
        AlertDialog(
            onDismissRequest = { pageToDelete = null },
            title = { Text(if (isFinalPage) "Delete final page?" else "Remove page ${targetPage.position + 1}?") },
            text = {
                Text(
                    if (isFinalPage) "Removing the last remaining page will move the document to local Trash."
                    else "This page will be removed from the document."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = targetPage.id
                        pageToDelete = null
                        onDeletePage(targetId)
                    }
                ) {
                    Text(if (isFinalPage) "Move to Trash" else "Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename document") },
        text = { TextField(name, { name = it }, label = { Text("Document name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MoveDialog(currentFolderId: FolderId?, folders: List<Folder>, onDismiss: () -> Unit, onConfirm: (FolderId?) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(currentFolderId?.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            LazyColumn {
                item { FolderChoice("No folder (Unfiled)", selected == null) { selected = null } }
                items(folders, key = { it.id.value }) { folder -> FolderChoice(folder.name, selected == folder.id.value) { selected = folder.id.value } }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected?.let(::FolderId)) }) { Text("Move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FolderChoice(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) { Text(name, Modifier.padding(16.dp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
}

@Composable
private fun MissingDocument(modifier: Modifier, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Description, null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Document unavailable", style = MaterialTheme.typography.titleLarge)
        Text("It may have been removed from this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back to Home") }
    }
}

@Composable
fun PageViewerScreen(
    document: Document,
    initialPage: Int,
    fileStore: DocumentFileStore,
    onBack: () -> Unit
) {
    var pageIndex by rememberSaveable(document.id.value) { mutableStateOf(initialPage.coerceIn(0, document.pages.lastIndex)) }
    val page = document.pages[pageIndex]
    var scale by remember(page.id.value) { mutableFloatStateOf(1f) }
    var offset by remember(page.id.value) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        offset = if (nextScale == 1f) Offset.Zero else offset + panChange
    }
    val bitmapState by rememberManagedBitmap(fileStore, page.processedAsset, 2560, page.rotationDegrees)

    Box(Modifier.fillMaxSize().background(OScanTheme.colors.workspace)) {
        when (val image = bitmapState) {
            ManagedBitmap.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ManagedBitmap.Missing -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BrokenImage, null, tint = OScanTheme.colors.onWorkspace, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("This page image is missing", color = OScanTheme.colors.onWorkspace)
            }
            is ManagedBitmap.Ready -> Image(
                bitmap = image.bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1} of ${document.pages.size}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                    .transformable(transformState)
                    .pointerInput(page.id.value) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                        })
                    }
            )
        }
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { pageIndex--; scale = 1f; offset = Offset.Zero }, enabled = pageIndex > 0) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, "Previous page")
                }
                Text("${pageIndex + 1} of ${document.pages.size}", modifier = Modifier.padding(horizontal = 12.dp))
                IconButton(onClick = { pageIndex++; scale = 1f; offset = Offset.Zero }, enabled = pageIndex < document.pages.lastIndex) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, "Next page")
                }
            }
        }
    }
}

private sealed interface ManagedBitmap {
    data object Loading : ManagedBitmap
    data object Missing : ManagedBitmap
    data class Ready(val bitmap: Bitmap) : ManagedBitmap
}

@Composable
private fun Thumbnail(
    path: String?,
    fileStore: DocumentFileStore,
    description: String,
    modifier: Modifier,
    rotationDegrees: Int = 0
) {
    val bitmapState by rememberManagedBitmap(fileStore, path, 720, rotationDegrees)
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when (val image = bitmapState) {
            ManagedBitmap.Loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            ManagedBitmap.Missing -> Icon(Icons.Default.BrokenImage, "Thumbnail unavailable", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            is ManagedBitmap.Ready -> Image(image.bitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun rememberManagedBitmap(fileStore: DocumentFileStore, maxDimension: Int) =
    produceState<ManagedBitmap>(initialValue = ManagedBitmap.Loading, maxDimension) {
        value = ManagedBitmap.Missing
    }

@Composable
private fun rememberManagedBitmap(
    fileStore: DocumentFileStore,
    path: String?,
    maxDimension: Int,
    rotationDegrees: Int = 0
) =
    produceState<ManagedBitmap>(initialValue = ManagedBitmap.Loading, path, maxDimension, rotationDegrees) {
        value = withContext(Dispatchers.IO) {
            if (path == null) return@withContext ManagedBitmap.Missing
            runCatching {
                val file = fileStore.resolve(path)
                if (!file.isFile) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
                val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?: return@runCatching null
                val normalizedRotation = (rotationDegrees % 360 + 360) % 360
                if (normalizedRotation == 0) decoded
                else {
                    val matrix = android.graphics.Matrix().apply { postRotate(normalizedRotation.toFloat()) }
                    val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                    if (rotated !== decoded) decoded.recycle()
                    rotated
                }
            }.getOrNull()?.let(ManagedBitmap::Ready) ?: ManagedBitmap.Missing
        }
    }

@Composable
fun EmptyStateLayout(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, supportingText: String, actions: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.size(96.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(44.dp)) }
            }
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(supportingText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            actions()
        }
    }
}

private fun Document.metadataLine(): String {
    val formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(modifiedAt.atZone(ZoneId.systemDefault()).toLocalDate())
    val pagesLabel = if (pages.size == 1) "1 page" else "${pages.size} pages"
    return "$formattedDate • $pagesLabel"
}

private fun Page.safeAspectRatio(): Float =
    if (width > 0 && height > 0) (width.toFloat() / height.toFloat()).coerceIn(0.55f, 1.6f) else 0.75f

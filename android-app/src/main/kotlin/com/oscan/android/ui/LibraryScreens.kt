package com.oscan.android.ui

import com.oscan.android.R

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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.LaunchedEffect
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
import com.oscan.android.data.preferences.JpegQuality
import com.oscan.android.data.preferences.PdfPageSize
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.engine.AndroidDocumentExporter
import com.oscan.android.engine.ExportFormat
import com.oscan.android.engine.PdfPageSpec
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
    onOpenVault: (() -> Unit)? = null,
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
                placeholder = { Text(stringResource(R.string.library_search_names)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search)) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear_search))
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
            onOpenVault = onOpenVault,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.documents.isEmpty() && state.searchQuery.isNotEmpty() -> NoSearchResultsState(state.searchQuery)
            state.documents.isEmpty() -> {
                if (state.filter == DocumentFilter.FAVORITES) {
                    EmptyFavoritesState()
                } else {
                    emptyContent()
                }
            }
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

/**
 * Displays an empty state layout when the Favorites filter is active and no favorite documents exist.
 * Explains how to mark documents as favorite and does not include the root scan document action button.
 */
@Composable
private fun EmptyFavoritesState() {
    EmptyStateLayout(
        icon = Icons.Default.Favorite,
        title = stringResource(R.string.favorites_empty_title),
        supportingText = stringResource(R.string.favorites_empty_body)
    ) {}
}

@Composable
private fun FilterChipsRow(
    currentFilter: DocumentFilter,
    onFilterChange: (DocumentFilter) -> Unit,
    foldersCount: Int,
    onOpenVault: (() -> Unit)? = null,
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
                label = { Text(stringResource(R.string.filter_all)) }
            )
        }
        item {
            FilterChip(
                selected = currentFilter == DocumentFilter.FAVORITES,
                onClick = { onFilterChange(DocumentFilter.FAVORITES) },
                label = { Text(stringResource(R.string.filter_favorites)) },
                leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
            )
        }
        if (onOpenVault != null) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onOpenVault,
                    label = { Text(stringResource(R.string.filter_vault)) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun NoSearchResultsState(query: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.library_no_matches), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.library_no_matches_query, query), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.library_recent)) }
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
                SectionTitle(stringResource(R.string.library_all_documents), Modifier.padding(top = 12.dp))
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
            item { SectionTitle(stringResource(R.string.library_recent), Modifier.padding(horizontal = 16.dp)) }
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
            item { SectionTitle(stringResource(R.string.library_all_documents), Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp)) }
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
                    description = stringResource(R.string.cd_document_thumbnail, document.name),
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
                            Icon(
                                Icons.Filled.Favorite,
                                stringResource(R.string.favorite),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
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
            description = stringResource(R.string.cd_document_thumbnail, document.name),
            modifier = Modifier.size(width = 72.dp, height = 96.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(document.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(document.metadataLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            document.folder?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (document.isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                stringResource(R.string.favorite),
                tint = MaterialTheme.colorScheme.primary
            )
        }
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
                title = { Text(stringResource(R.string.folders_title)) },
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
                    title = stringResource(R.string.folders_empty_title),
                    supportingText = stringResource(R.string.folders_empty_body)
                ) {
                    Button(onClick = { createDialogOpen = true }) {
                        Icon(Icons.Default.CreateNewFolder, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.folder_create))
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
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_delete)) },
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
                            DropdownMenuItem(text = { Text(stringResource(R.string.folder_rename)) }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menuOpen = false; renameOpen = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.folder_delete)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuOpen = false; deleteOpen = true })
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
                    title = stringResource(R.string.folder_empty_title),
                    supportingText = stringResource(R.string.folder_empty_body)
                ) {
                    Button(onClick = onBack) { Text(stringResource(R.string.action_back_folders)) }
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
                title = { Text(stringResource(R.string.trash_title_count, trashDocuments.size)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (trashDocuments.isNotEmpty()) {
                        TextButton(onClick = { emptyTrashDialogOpen = true }) {
                            Text(stringResource(R.string.vault_empty_trash), color = MaterialTheme.colorScheme.error)
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
                    title = stringResource(R.string.library_trash_empty),
                    supportingText = stringResource(R.string.trash_empty_body)
                ) {
                    Button(onClick = onBack) { Text(stringResource(R.string.action_back_home)) }
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
            title = { Text(stringResource(R.string.trash_empty_confirm)) },
            text = { Text(stringResource(R.string.trash_empty_confirm_body, trashDocuments.size)) },
            confirmButton = {
                TextButton(onClick = { emptyTrashDialogOpen = false; onEmptyTrash() }) {
                    Text(stringResource(R.string.vault_empty_trash), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { emptyTrashDialogOpen = false }) { Text(stringResource(R.string.action_cancel)) } }
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
            description = stringResource(R.string.cd_document_thumbnail, document.name),
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
        title = { Text(stringResource(R.string.folder_new)) },
        text = { TextField(name, { name = it }, label = { Text(stringResource(R.string.folder_name)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun RenameFolderDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_rename)) },
        text = { TextField(name, { name = it }, label = { Text(stringResource(R.string.folder_name)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank() && name.trim() != currentName) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun DeleteFolderDialog(folderName: String, documentCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_delete_title, folderName)) },
        text = { Text(stringResource(R.string.folder_delete_body, documentCount)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.folder_delete_action), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
    onCreateFolder: (String) -> Unit = {},
    onTrash: () -> Unit,
    defaultJpegQuality: com.oscan.android.data.preferences.JpegQuality = com.oscan.android.data.preferences.JpegQuality.HIGH,
    defaultPageSize: PdfPageSize = PdfPageSize.A4,
    onSavePdf: (Context, Document, Uri) -> Unit = { _, _, _ -> },
    onSharePdf: (Context, Document) -> Unit = { _, _ -> },
    onExport: (Context, Document, Uri, com.oscan.android.engine.ExportFormat, com.oscan.android.data.preferences.JpegQuality) -> Unit = { ctx, doc, uri, fmt, q -> onSavePdf(ctx, doc, uri) },
    onShare: (Context, Document, com.oscan.android.engine.ExportFormat, com.oscan.android.data.preferences.JpegQuality) -> Unit = { ctx, doc, fmt, q -> onSharePdf(ctx, doc) },
    onOpenPage: (Int) -> Unit,
    onReorderPages: (List<PageId>) -> Unit = {},
    onRotatePage: (PageId, Int) -> Unit = { _, _ -> },
    onDeletePage: (PageId) -> Unit = {},
    onAddPages: () -> Unit = {},
    onEditPage: (Page) -> Unit = {},
    onMoveToVault: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val compactActions = configuration.screenWidthDp < 600 || configuration.fontScale >= 1.5f
    var overflowOpen by remember { mutableStateOf(false) }
    var renameOpen by rememberSaveable { mutableStateOf(false) }
    var moveOpen by rememberSaveable { mutableStateOf(false) }
    var createFolderOpen by rememberSaveable { mutableStateOf(false) }
    var trashOpen by rememberSaveable { mutableStateOf(false) }
    var exportDialogOpen by rememberSaveable { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf(com.oscan.android.engine.ExportFormat.PDF) }
    var pendingExportQuality by remember { mutableStateOf(defaultJpegQuality) }
    var pageToDelete by remember { mutableStateOf<Page?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && document != null) {
            onExport(context, document, uri, pendingExportFormat, pendingExportQuality)
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
                                onClick = { exportDialogOpen = true },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, "Export / Save")
                            }
                        }
                        IconButton(onClick = { onFavorite(!document.isFavorite) }) {
                            Icon(
                                if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                stringResource(
                                    if (document.isFavorite) R.string.favorite_remove else R.string.favorite_add
                                )
                            )
                        }
                        IconButton(onClick = { renameOpen = true }) {
                            Icon(Icons.Default.Edit, "Rename")
                        }
                        Box {
                            IconButton(onClick = { overflowOpen = true }) {
                                Icon(Icons.Default.MoreVert, "More actions")
                            }
                            DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                                DropdownMenuItem(
            text = { Text(stringResource(R.string.action_add_pages)) },
                                    leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, null) },
                                    onClick = { overflowOpen = false; onAddPages() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.document_export_save)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                                    onClick = { overflowOpen = false; exportDialogOpen = true },
                                    enabled = !isExporting
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (document.isFavorite) R.string.favorite_remove else R.string.favorite_add
                                            )
                                        )
                                    },
                                    leadingIcon = { Icon(if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null) },
                                    onClick = { overflowOpen = false; onFavorite(!document.isFavorite) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_move_to_vault)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    onClick = { overflowOpen = false; onMoveToVault() }
                                )
                                if (folders.isNotEmpty() || document.folder != null) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.document_move_folder)) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) }, onClick = { overflowOpen = false; moveOpen = true })
                                }
                                DropdownMenuItem(text = { Text(stringResource(R.string.action_move_to_trash)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { overflowOpen = false; trashOpen = true })
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
                    .clickable(onClickLabel = stringResource(R.string.document_rename)) { renameOpen = true }
                                    .padding(vertical = 4.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(document.metadataLine(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            document.folder?.let { Text(stringResource(R.string.document_folder_name, it.name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.document_pages), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = onAddPages) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.document_add_page))
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
                                        description = stringResource(R.string.scanner_page_number, position + 1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(page.safeAspectRatio())
                                            .clickable { onOpenPage(position) },
                                        rotationDegrees = page.rotationDegrees
                                    )
                                    var pageMenuOpen by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { pageMenuOpen = true },
                                            modifier = Modifier.background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                        ) {
                                            Icon(Icons.Default.MoreVert, "Page options")
                                        }
                                        DropdownMenu(expanded = pageMenuOpen, onDismissRequest = { pageMenuOpen = false }) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.page_rotate_left)) },
                                                leadingIcon = { Icon(Icons.Default.RotateLeft, null) },
                                                onClick = { pageMenuOpen = false; onRotatePage(page.id, -90) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.page_rotate_right)) },
                                                leadingIcon = { Icon(Icons.Default.RotateRight, null) },
                                                onClick = { pageMenuOpen = false; onRotatePage(page.id, 90) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.page_recrop_treatment)) },
                                                leadingIcon = { Icon(Icons.Default.Crop, null) },
                                                onClick = { pageMenuOpen = false; onEditPage(page) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.page_remove)) },
                                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                onClick = { pageMenuOpen = false; pageToDelete = page }
                                            )
                                        }
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                                        stringResource(R.string.scanner_page_number, position + 1),
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
                        Text(stringResource(R.string.document_exporting), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (exportDialogOpen && document != null) {
        ExportAndSaveDialog(
            document = document,
            fileStore = fileStore,
            defaultQuality = defaultJpegQuality,
            pageSize = defaultPageSize,
            isExporting = isExporting,
            onDismiss = { exportDialogOpen = false },
            onSave = { format, quality ->
                exportDialogOpen = false
                pendingExportFormat = format
                pendingExportQuality = quality
                val ext = if (document.pages.size == 1) format.extension else if (format == com.oscan.android.engine.ExportFormat.PDF) "pdf" else "zip"
                createDocumentLauncher.launch("${document.name}.$ext")
            },
            onShare = { format, quality ->
                exportDialogOpen = false
                onShare(context, document, format, quality)
            }
        )
    }

    if (renameOpen && document != null) RenameDialog(document.name, { renameOpen = false }) { onRename(it); renameOpen = false }
    if (moveOpen && document != null) MoveDialog(
        currentFolderId = document.folder?.id,
        folders = folders,
        onDismiss = { moveOpen = false },
        onCreateFolder = { moveOpen = false; createFolderOpen = true },
        onConfirm = { onMove(it); moveOpen = false }
    )
    if (createFolderOpen) CreateFolderDialog(
        onDismiss = { createFolderOpen = false },
        onConfirm = { name ->
            onCreateFolder(name)
            createFolderOpen = false
            moveOpen = true
        }
    )
    if (trashOpen) AlertDialog(
        onDismissRequest = { trashOpen = false },
        title = { Text(stringResource(R.string.document_trash_title)) },
        text = { Text(stringResource(R.string.document_trash_body)) },
        confirmButton = { TextButton(onClick = { trashOpen = false; onTrash() }) { Text(stringResource(R.string.action_move_to_trash)) } },
        dismissButton = { TextButton(onClick = { trashOpen = false }) { Text(stringResource(R.string.action_cancel)) } }
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
                    Text(stringResource(R.string.action_cancel))
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
        title = { Text(stringResource(R.string.document_rename)) },
        text = { TextField(name, { name = it }, label = { Text(stringResource(R.string.scanner_document_name)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun MoveDialog(
    currentFolderId: FolderId?,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onCreateFolder: () -> Unit = {},
    onConfirm: (FolderId?) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(currentFolderId?.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.document_move_folder)) },
        text = {
            LazyColumn {
                item { FolderChoice(stringResource(R.string.scanner_no_folder), selected == null) { selected = null } }
                items(folders, key = { it.id.value }) { folder -> FolderChoice(folder.name, selected == folder.id.value) { selected = folder.id.value } }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onCreateFolder)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.folder_create_new))
                        }
                    }
                }
            }
        },
            confirmButton = { TextButton(onClick = { onConfirm(selected?.let(::FolderId)) }) { Text(stringResource(R.string.action_move)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
        Text(stringResource(R.string.document_unavailable), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.document_unavailable_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.action_back_home)) }
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
                Text(stringResource(R.string.page_image_missing), color = OScanTheme.colors.onWorkspace)
            }
            is ManagedBitmap.Ready -> Image(
                bitmap = image.bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.page_position_format, pageIndex + 1, document.pages.size),
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
                Text(stringResource(R.string.page_count_position, pageIndex + 1, document.pages.size), modifier = Modifier.padding(horizontal = 12.dp))
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
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(supportingText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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

@Composable
fun ExportAndSaveDialog(
    document: Document,
    fileStore: DocumentFileStore,
    defaultQuality: com.oscan.android.data.preferences.JpegQuality = com.oscan.android.data.preferences.JpegQuality.HIGH,
    pageSize: PdfPageSize = PdfPageSize.A4,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onSave: (com.oscan.android.engine.ExportFormat, com.oscan.android.data.preferences.JpegQuality) -> Unit,
    onShare: (com.oscan.android.engine.ExportFormat, com.oscan.android.data.preferences.JpegQuality) -> Unit
) {
    var selectedFormat by rememberSaveable { mutableStateOf(com.oscan.android.engine.ExportFormat.PDF) }
    var selectedQuality by rememberSaveable { mutableStateOf(defaultQuality) }
    var sizeEstimates by remember { mutableStateOf<Map<ExportFormat, Map<JpegQuality, Long>>?>(null) }
    val documentExporter = remember { AndroidDocumentExporter() }

    LaunchedEffect(document.id, document.modifiedAt, pageSize) {
        sizeEstimates = null
        sizeEstimates = withContext(Dispatchers.IO) {
            runCatching {
                val pages = document.pages.sortedBy { it.position }.mapNotNull { page ->
                    val file = fileStore.resolve(page.processedAsset).takeIf { it.isFile }
                        ?: fileStore.resolve(page.originalAsset).takeIf { it.isFile }
                    file?.let { PdfPageSpec(it, page.rotationDegrees) }
                }
                require(pages.isNotEmpty()) { "No page assets found for document" }
                ExportFormat.entries.associateWith { format ->
                    if (format == ExportFormat.PNG) {
                        val size = documentExporter.measureDocumentSize(
                            pages = pages,
                            format = format,
                            pageSize = pageSize,
                            quality = JpegQuality.HIGH,
                            documentName = document.name
                        )
                        JpegQuality.entries.associateWith { size }
                    } else {
                        JpegQuality.entries.associateWith { quality ->
                            documentExporter.measureDocumentSize(
                                pages = pages,
                                format = format,
                                pageSize = pageSize,
                                quality = quality,
                                documentName = document.name
                            )
                        }
                    }
                }
            }.getOrElse { emptyMap() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.document_export_save))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.document_export_options_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    stringResource(R.string.export_format),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.oscan.android.engine.ExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = {
                                Text(
                                    format.label,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            },
                            leadingIcon = {
                                val icon = when (format) {
                                    com.oscan.android.engine.ExportFormat.PDF -> Icons.Default.PictureAsPdf
                                    com.oscan.android.engine.ExportFormat.PNG -> Icons.Default.Image
                                    com.oscan.android.engine.ExportFormat.JPG -> Icons.Default.Photo
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    stringResource(R.string.export_image_quality),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (selectedFormat == ExportFormat.PNG) {
                    Text(
                        text = stringResource(R.string.export_png_quality_fixed),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.oscan.android.data.preferences.JpegQuality.entries.forEach { quality ->
                            FilterChip(
                                selected = selectedQuality == quality,
                                onClick = { selectedQuality = quality },
                                label = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = quality.label(),
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = when (val estimates = sizeEstimates) {
                                                null -> stringResource(R.string.export_size_calculating)
                                                else -> estimates[selectedFormat]?.get(quality)?.let(::formatFileSize)
                                                    ?: stringResource(R.string.export_size_unavailable)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onShare(selectedFormat, selectedQuality) },
                    enabled = !isExporting
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share))
                }
                Button(
                    onClick = { onSave(selectedFormat, selectedQuality) },
                    enabled = !isExporting
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}

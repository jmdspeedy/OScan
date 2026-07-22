package com.oscan.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.model.Page
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
    onOpenDocument: (com.oscan.android.data.model.DocumentId) -> Unit,
    emptyContent: @Composable () -> Unit
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.documents.isEmpty() -> emptyContent()
        state.presentation == LibraryPresentation.GRID -> DocumentGrid(
            state = state,
            fileStore = fileStore,
            gridState = gridState,
            onOpenDocument = onOpenDocument
        )
        else -> DocumentList(
            state = state,
            fileStore = fileStore,
            listState = listState,
            onOpenDocument = onOpenDocument
        )
    }
}

@Composable
private fun DocumentGrid(
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    gridState: LazyGridState,
    onOpenDocument: (com.oscan.android.data.model.DocumentId) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        state = gridState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Recent") }
        items(state.recentDocuments, key = { "recent-${it.id.value}" }) { document ->
            DocumentCard(document, fileStore, onOpenDocument)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle("All documents", Modifier.padding(top = 12.dp))
        }
        items(state.documents, key = { "all-${it.id.value}" }) { document ->
            DocumentCard(document, fileStore, onOpenDocument)
        }
    }
}

@Composable
private fun DocumentList(
    state: LibraryUiState,
    fileStore: DocumentFileStore,
    listState: LazyListState,
    onOpenDocument: (com.oscan.android.data.model.DocumentId) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        item { SectionTitle("Recent", Modifier.padding(horizontal = 16.dp)) }
        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.recentDocuments, key = { it.id.value }) { document ->
                    Box(Modifier.width(156.dp)) { DocumentCard(document, fileStore, onOpenDocument) }
                }
            }
        }
        item { SectionTitle("All documents", Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp)) }
        items(state.documents, key = { it.id.value }) { document ->
            DocumentRow(document, fileStore, onOpenDocument)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(bottom = 12.dp))
}

@Composable
private fun DocumentCard(
    document: Document,
    fileStore: DocumentFileStore,
    onOpenDocument: (com.oscan.android.data.model.DocumentId) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onOpenDocument(document.id) }
    ) {
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
    }
}

@Composable
private fun DocumentRow(
    document: Document,
    fileStore: DocumentFileStore,
    onOpenDocument: (com.oscan.android.data.model.DocumentId) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpenDocument(document.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
fun DocumentDetailScreen(
    document: Document?,
    requestedDocumentExists: Boolean,
    folders: List<Folder>,
    fileStore: DocumentFileStore,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onFavorite: (Boolean) -> Unit,
    onMove: (FolderId?) -> Unit,
    onTrash: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var renameOpen by rememberSaveable { mutableStateOf(false) }
    var moveOpen by rememberSaveable { mutableStateOf(false) }
    var trashOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document?.name ?: "Document", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (document != null) {
                        IconButton(onClick = { onFavorite(!document.isFavorite) }) {
                            Icon(if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (document.isFavorite) "Remove favorite" else "Add favorite")
                        }
                        Box {
                            IconButton(onClick = { overflowOpen = true }) { Icon(Icons.Default.MoreVert, "Document actions") }
                            DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { overflowOpen = false; renameOpen = true })
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
        when {
            !requestedDocumentExists -> MissingDocument(Modifier.padding(padding), onBack)
            document == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(144.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(document.metadataLine(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        document.folder?.let { Text("Folder: ${it.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.height(8.dp))
                        Text("Pages", style = MaterialTheme.typography.titleMedium)
                    }
                }
                items(document.pages, key = { it.id.value }) { page ->
                    val position = page.position
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.clickable { onOpenPage(position) }
                    ) {
                        Column {
                            Thumbnail(page.thumbnailAsset, fileStore, "Page ${position + 1}", Modifier.fillMaxWidth().aspectRatio(page.safeAspectRatio()))
                            Text("Page ${position + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(12.dp))
                        }
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
private fun MoveDialog(currentFolderId: FolderId?, folders: List<Folder>, onDismiss: () -> Unit, onConfirm: (FolderId?) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(currentFolderId?.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            LazyColumn {
                item { FolderChoice("No folder", selected == null) { selected = null } }
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
    val bitmapState by rememberManagedBitmap(fileStore, page.processedAsset, 2560)

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
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
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
private fun Thumbnail(path: String?, fileStore: DocumentFileStore, description: String, modifier: Modifier) {
    val bitmapState by rememberManagedBitmap(fileStore, path, 720)
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when (val image = bitmapState) {
            ManagedBitmap.Loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            ManagedBitmap.Missing -> Icon(Icons.Default.BrokenImage, "Thumbnail unavailable", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            is ManagedBitmap.Ready -> Image(image.bitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun rememberManagedBitmap(fileStore: DocumentFileStore, path: String?, maxDimension: Int) =
    produceState<ManagedBitmap>(initialValue = ManagedBitmap.Loading, path, maxDimension) {
        value = withContext(Dispatchers.IO) {
            if (path == null) return@withContext ManagedBitmap.Missing
            runCatching {
                val file = fileStore.resolve(path)
                if (!file.isFile) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()?.let(ManagedBitmap::Ready) ?: ManagedBitmap.Missing
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

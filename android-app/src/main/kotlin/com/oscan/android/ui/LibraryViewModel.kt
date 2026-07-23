package com.oscan.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.model.Page
import com.oscan.android.data.model.PageId
import com.oscan.android.data.preferences.DocumentSort
import com.oscan.android.data.preferences.LibraryPresentation
import com.oscan.android.data.preferences.UserPreferences
import com.oscan.android.data.preferences.UserPreferencesStore
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.RepositoryError
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.engine.AndroidPdfExporter
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DocumentFilter { ALL, FAVORITES }

data class LibraryUiState(
    val isLoading: Boolean = true,
    val documents: List<Document> = emptyList(),
    val recentDocuments: List<Document> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val trashDocuments: List<Document> = emptyList(),
    val selectedDocument: Document? = null,
    val selectedDocumentId: DocumentId? = null,
    val selectedFolderId: FolderId? = null,
    val isViewingTrash: Boolean = false,
    val searchQuery: String = "",
    val filter: DocumentFilter = DocumentFilter.ALL,
    val selectionMode: Boolean = false,
    val selectedDocumentIds: Set<DocumentId> = emptySet(),
    val presentation: LibraryPresentation = LibraryPresentation.GRID,
    val sort: DocumentSort = DocumentSort.MODIFIED_DESC,
    val isExporting: Boolean = false,
    val message: String? = null,
    val userPreferences: UserPreferences = UserPreferences()
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: DocumentRepository,
    private val preferencesStore: UserPreferencesStore,
    private val pdfExporter: AndroidPdfExporter = AndroidPdfExporter()
) : ViewModel() {
    private val selectedDocumentId = MutableStateFlow<DocumentId?>(null)
    private val selectedFolderId = MutableStateFlow<FolderId?>(null)
    private val isViewingTrash = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(DocumentFilter.ALL)
    private val selectionMode = MutableStateFlow(false)
    private val selectedDocumentIds = MutableStateFlow<Set<DocumentId>>(emptySet())
    private val isExporting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val selectedDocument = selectedDocumentId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeDocument(id)
    }

    val uiState = combine(
        repository.observeDocuments(),
        repository.observeFolders(),
        repository.observeTrash(),
        preferencesStore.preferences,
        selectedDocument,
        selectedFolderId,
        isViewingTrash,
        searchQuery,
        filter,
        selectionMode,
        selectedDocumentIds,
        isExporting,
        message
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val documents = flows[0] as List<Document>
        @Suppress("UNCHECKED_CAST")
        val folders = flows[1] as List<Folder>
        @Suppress("UNCHECKED_CAST")
        val trashDocs = flows[2] as List<Document>
        val preferences = flows[3] as UserPreferences
        val selectedDoc = flows[4] as Document?
        val currentFolderId = flows[5] as FolderId?
        val viewingTrash = flows[6] as Boolean
        val query = flows[7] as String
        val currentFilter = flows[8] as DocumentFilter
        val inSelectionMode = flows[9] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selectedIds = flows[10] as Set<DocumentId>
        val exporting = flows[11] as Boolean
        val currentMessage = flows[12] as String?

        var filteredDocs = documents

        // Filter by folder if selected
        if (currentFolderId != null) {
            filteredDocs = filteredDocs.filter { it.folder?.id == currentFolderId }
        }

        // Filter by chip
        filteredDocs = when (currentFilter) {
            DocumentFilter.ALL -> filteredDocs
            DocumentFilter.FAVORITES -> filteredDocs.filter { it.isFavorite }
        }

        // Filter by search query (document name or folder name)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filteredDocs = filteredDocs.filter { doc ->
                doc.name.lowercase().contains(q) || (doc.folder?.name?.lowercase()?.contains(q) == true)
            }
        }

        val sorted = filteredDocs.sortedWith(preferences.documentSort.comparator())
        LibraryUiState(
            isLoading = false,
            documents = sorted,
            recentDocuments = documents.sortedByDescending(Document::modifiedAt).take(5),
            folders = folders,
            trashDocuments = trashDocs,
            selectedDocument = selectedDoc,
            selectedDocumentId = selectedDocumentId.value,
            selectedFolderId = currentFolderId,
            isViewingTrash = viewingTrash,
            searchQuery = query,
            filter = currentFilter,
            selectionMode = inSelectionMode,
            selectedDocumentIds = selectedIds,
            presentation = preferences.libraryPresentation,
            sort = preferences.documentSort,
            isExporting = exporting,
            message = currentMessage,
            userPreferences = preferences
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun openDocument(id: DocumentId) {
        selectedDocumentId.value = id
        message.value = null
    }

    fun closeDocument() {
        selectedDocumentId.value = null
        message.value = null
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(newFilter: DocumentFilter) {
        filter.value = newFilter
    }

    fun openFolder(id: FolderId?) {
        selectedFolderId.value = id
        clearSelection()
    }

    fun closeFolder() {
        selectedFolderId.value = null
        clearSelection()
    }

    fun openTrash() {
        isViewingTrash.value = true
        clearSelection()
    }

    fun closeTrash() {
        isViewingTrash.value = false
        clearSelection()
    }

    fun setPresentation(presentation: LibraryPresentation) {
        viewModelScope.launch { preferencesStore.setLibraryPresentation(presentation) }
    }

    fun setSort(sort: DocumentSort) {
        viewModelScope.launch { preferencesStore.setDocumentSort(sort) }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch { preferencesStore.setDisplayName(name) }
    }

    fun setAvatarPreset(preset: String) {
        viewModelScope.launch { preferencesStore.setAvatarPreset(preset) }
    }

    fun setAutoCaptureDefault(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setAutoCaptureDefault(enabled) }
    }

    fun setShutterFeedback(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setShutterFeedback(enabled) }
    }

    fun setCameraLensPreference(preference: com.oscan.android.data.preferences.CameraLensPreference) {
        viewModelScope.launch { preferencesStore.setCameraLensPreference(preference) }
    }

    fun setDefaultTreatment(treatment: String) {
        viewModelScope.launch { preferencesStore.setDefaultTreatment(treatment) }
    }

    fun setDefaultExportFilenamePattern(pattern: String) {
        viewModelScope.launch { preferencesStore.setDefaultExportFilenamePattern(pattern) }
    }

    fun setDefaultPageSize(pageSize: com.oscan.android.data.preferences.PdfPageSize) {
        viewModelScope.launch { preferencesStore.setDefaultPageSize(pageSize) }
    }

    fun setDefaultJpegQuality(quality: com.oscan.android.data.preferences.JpegQuality) {
        viewModelScope.launch { preferencesStore.setDefaultJpegQuality(quality) }
    }

    fun setThemeChoice(themeChoice: com.oscan.android.data.preferences.ThemeChoice) {
        viewModelScope.launch { preferencesStore.setThemeChoice(themeChoice) }
    }

    fun cleanCache(context: Context, fileStore: DocumentFileStore) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    fileStore.clearTempFiles()
                    context.cacheDir.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".tmp") || file.name.endsWith(".jpg") || file.name.endsWith(".png") || (file.isDirectory && file.name == "pdfs")) {
                            file.deleteRecursively()
                        }
                    }
                }
            }
            message.value = "Cache cleaned"
        }
    }

    fun rename(name: String) = mutateSelected { repository.rename(it, name) }

    fun setFavorite(favorite: Boolean) = mutateSelected { repository.setFavorite(it, favorite) }

    fun moveToFolder(folderId: FolderId?) = mutateSelected { repository.moveToFolder(it, folderId) }

    fun moveToTrash() {
        val id = selectedDocumentId.value ?: return
        viewModelScope.launch {
            runCatching { repository.moveToTrash(id) }
                .onSuccess {
                    selectedDocumentId.value = null
                    message.value = "Document moved to Trash"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            runCatching { repository.createFolder(name) }
                .onSuccess { message.value = "Folder created" }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun renameFolder(id: FolderId, name: String) {
        viewModelScope.launch {
            runCatching { repository.renameFolder(id, name) }
                .onSuccess { message.value = "Folder renamed" }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun deleteFolder(id: FolderId) {
        viewModelScope.launch {
            runCatching { repository.deleteFolder(id) }
                .onSuccess {
                    if (selectedFolderId.value == id) selectedFolderId.value = null
                    message.value = "Folder deleted. Documents moved to Unfiled."
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun toggleSelectionMode(initialSelectedId: DocumentId? = null) {
        if (selectionMode.value) {
            selectionMode.value = false
            selectedDocumentIds.value = emptySet()
        } else {
            selectionMode.value = true
            selectedDocumentIds.value = if (initialSelectedId != null) setOf(initialSelectedId) else emptySet()
        }
    }

    fun toggleDocumentSelection(id: DocumentId) {
        val current = selectedDocumentIds.value
        val updated = if (current.contains(id)) current - id else current + id
        selectedDocumentIds.value = updated
        if (updated.isEmpty()) {
            selectionMode.value = false
        }
    }

    fun selectAll(documentIds: List<DocumentId>) {
        if (selectedDocumentIds.value.size == documentIds.size) {
            selectedDocumentIds.value = emptySet()
        } else {
            selectedDocumentIds.value = documentIds.toSet()
        }
    }

    fun clearSelection() {
        selectionMode.value = false
        selectedDocumentIds.value = emptySet()
    }

    fun bulkMoveToTrash() {
        val ids = selectedDocumentIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.bulkMoveToTrash(ids) }
                .onSuccess {
                    val count = ids.size
                    clearSelection()
                    message.value = if (count == 1) "Document moved to Trash" else "$count documents moved to Trash"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun bulkMoveToFolder(folderId: FolderId?) {
        val ids = selectedDocumentIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.bulkMoveToFolder(ids, folderId) }
                .onSuccess {
                    clearSelection()
                    message.value = "Documents moved"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun bulkSetFavorite(favorite: Boolean) {
        val ids = selectedDocumentIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.bulkSetFavorite(ids, favorite) }
                .onSuccess {
                    clearSelection()
                    message.value = if (favorite) "Added to Favorites" else "Removed from Favorites"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun restoreDocument(id: DocumentId) {
        viewModelScope.launch {
            runCatching { repository.restore(id) }
                .onSuccess { message.value = "Document restored" }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun permanentlyDeleteDocument(id: DocumentId) {
        viewModelScope.launch {
            runCatching { repository.permanentlyDelete(id) }
                .onSuccess { message.value = "Document permanently deleted" }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun restoreMultiple(ids: List<DocumentId>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.restoreMultiple(ids) }
                .onSuccess {
                    clearSelection()
                    message.value = "Documents restored"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun permanentlyDeleteMultiple(ids: List<DocumentId>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.permanentlyDeleteMultiple(ids) }
                .onSuccess {
                    clearSelection()
                    message.value = "Documents permanently deleted"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            runCatching { repository.emptyTrash() }
                .onSuccess {
                    clearSelection()
                    message.value = "Trash emptied"
                }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun reorderPages(documentId: DocumentId, pageIdsInOrder: List<PageId>) {
        viewModelScope.launch {
            runCatching { repository.reorderPages(documentId, pageIdsInOrder) }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun rotatePage(documentId: DocumentId, pageId: PageId, deltaDegrees: Int) {
        viewModelScope.launch {
            runCatching { repository.rotatePage(documentId, pageId, deltaDegrees) }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun deletePage(documentId: DocumentId, pageId: PageId) {
        viewModelScope.launch {
            runCatching { repository.deletePage(documentId, pageId) }
                .onSuccess { message.value = "Page removed" }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun exportDocumentPdfToUri(
        context: Context,
        document: Document,
        fileStore: DocumentFileStore,
        targetUri: Uri
    ) {
        if (isExporting.value) return
        isExporting.value = true
        message.value = null
        val currentPrefs = uiState.value.userPreferences
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pageSpecs = document.pages.sortedBy { it.position }.mapNotNull { page ->
                        val file = fileStore.resolve(page.processedAsset).takeIf { it.isFile }
                            ?: fileStore.resolve(page.originalAsset).takeIf { it.isFile }
                        file?.let { com.oscan.android.engine.PdfPageSpec(it, page.rotationDegrees) }
                    }
                    if (pageSpecs.isEmpty()) {
                        throw IllegalStateException("No page assets found for document")
                    }
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        pdfExporter.exportPageSpecsToPdf(
                            pages = pageSpecs,
                            outputStream = out,
                            pageSize = currentPrefs.defaultPageSize,
                            quality = currentPrefs.defaultJpegQuality
                        )
                    } ?: throw IllegalStateException("Could not open output stream")
                }
                message.value = "PDF exported successfully"
            } catch (e: Exception) {
                message.value = "Export failed: ${e.userMessage()}"
            } finally {
                isExporting.value = false
            }
        }
    }

    fun shareDocumentPdf(
        context: Context,
        document: Document,
        fileStore: DocumentFileStore,
        onLaunchShare: (Intent) -> Unit
    ) {
        if (isExporting.value) return
        isExporting.value = true
        message.value = null
        val currentPrefs = uiState.value.userPreferences
        viewModelScope.launch {
            try {
                val chooserIntent = withContext(Dispatchers.IO) {
                    val pageSpecs = document.pages.sortedBy { it.position }.mapNotNull { page ->
                        val file = fileStore.resolve(page.processedAsset).takeIf { it.isFile }
                            ?: fileStore.resolve(page.originalAsset).takeIf { it.isFile }
                        file?.let { com.oscan.android.engine.PdfPageSpec(it, page.rotationDegrees) }
                    }
                    if (pageSpecs.isEmpty()) {
                        throw IllegalStateException("No page assets found for document")
                    }
                    val pdfDir = File(context.cacheDir, "pdfs")
                    if (!pdfDir.exists()) pdfDir.mkdirs()

                    val safeName = document.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val pdfFile = File(pdfDir, "$safeName.pdf")
                    FileOutputStream(pdfFile).use { out ->
                        pdfExporter.exportPageSpecsToPdf(
                            pages = pageSpecs,
                            outputStream = out,
                            pageSize = currentPrefs.defaultPageSize,
                            quality = currentPrefs.defaultJpegQuality
                        )
                    }

                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "com.oscan.android.fileprovider",
                        pdfFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    Intent.createChooser(shareIntent, "Share ${document.name}")
                }
                onLaunchShare(chooserIntent)
            } catch (e: Exception) {
                message.value = "Share failed: ${e.userMessage()}"
            } finally {
                isExporting.value = false
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun mutateSelected(block: suspend (DocumentId) -> Unit) {
        val id = selectedDocumentId.value ?: return
        viewModelScope.launch {
            runCatching { block(id) }
                .onSuccess { message.value = null }
                .onFailure { message.value = it.userMessage() }
        }
    }
}

private fun Throwable.userMessage(): String =
    if (this is RepositoryError) message ?: "That change could not be saved"
    else message ?: "Operation could not be completed"

private fun DocumentSort.comparator(): Comparator<Document> = when (this) {
    DocumentSort.MODIFIED_DESC -> compareByDescending(Document::modifiedAt)
    DocumentSort.MODIFIED_ASC -> compareBy(Document::modifiedAt)
    DocumentSort.CREATED_DESC -> compareByDescending(Document::createdAt)
    DocumentSort.CREATED_ASC -> compareBy(Document::createdAt)
    DocumentSort.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER, Document::name)
    DocumentSort.NAME_DESC -> compareByDescending<Document, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
}

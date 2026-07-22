package com.oscan.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.preferences.DocumentSort
import com.oscan.android.data.preferences.LibraryPresentation
import com.oscan.android.data.preferences.UserPreferences
import com.oscan.android.data.preferences.UserPreferencesStore
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.RepositoryError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val documents: List<Document> = emptyList(),
    val recentDocuments: List<Document> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val selectedDocument: Document? = null,
    val selectedDocumentId: DocumentId? = null,
    val presentation: LibraryPresentation = LibraryPresentation.GRID,
    val sort: DocumentSort = DocumentSort.MODIFIED_DESC,
    val message: String? = null
)

class LibraryViewModel(
    private val repository: DocumentRepository,
    private val preferencesStore: UserPreferencesStore
) : ViewModel() {
    private val selectedDocumentId = MutableStateFlow<DocumentId?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val selectedDocument = selectedDocumentId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeDocument(id)
    }

    val uiState = combine(
        repository.observeDocuments(),
        repository.observeFolders(),
        preferencesStore.preferences,
        selectedDocument,
        message
    ) { documents, folders, preferences, selected, currentMessage ->
        val sorted = documents.sortedWith(preferences.documentSort.comparator())
        LibraryUiState(
            isLoading = false,
            documents = sorted,
            recentDocuments = documents.sortedByDescending(Document::modifiedAt).take(5),
            folders = folders,
            selectedDocument = selected,
            selectedDocumentId = selectedDocumentId.value,
            presentation = preferences.libraryPresentation,
            sort = preferences.documentSort,
            message = currentMessage
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

    fun setPresentation(presentation: LibraryPresentation) {
        viewModelScope.launch { preferencesStore.setLibraryPresentation(presentation) }
    }

    fun setSort(sort: DocumentSort) {
        viewModelScope.launch { preferencesStore.setDocumentSort(sort) }
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

private fun DocumentSort.comparator(): Comparator<Document> = when (this) {
    DocumentSort.MODIFIED_DESC -> compareByDescending(Document::modifiedAt)
    DocumentSort.MODIFIED_ASC -> compareBy(Document::modifiedAt)
    DocumentSort.CREATED_DESC -> compareByDescending(Document::createdAt)
    DocumentSort.CREATED_ASC -> compareBy(Document::createdAt)
    DocumentSort.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER, Document::name)
    DocumentSort.NAME_DESC -> compareByDescending<Document, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
}

private fun Throwable.userMessage(): String =
    if (this is RepositoryError) message ?: "That change could not be saved"
    else "That change could not be saved"

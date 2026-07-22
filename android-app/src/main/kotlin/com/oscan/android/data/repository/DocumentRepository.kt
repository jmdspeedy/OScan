package com.oscan.android.data.repository

import androidx.room.withTransaction
import com.oscan.android.data.db.DocumentAggregate
import com.oscan.android.data.db.DocumentEntity
import com.oscan.android.data.db.DocumentFolderEntity
import com.oscan.android.data.db.LibraryDao
import com.oscan.android.data.db.OScanDatabase
import com.oscan.android.data.db.PageEntity
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.model.Page
import com.oscan.android.data.model.PageId
import com.oscan.android.data.storage.AssetKind
import com.oscan.android.data.storage.DocumentFileStore
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed class RepositoryError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidName : RepositoryError("Choose a document name")
    class EmptyDocument : RepositoryError("Add at least one page")
    class NotFound : RepositoryError("This document is no longer available")
    class AssetWrite(cause: Throwable) : RepositoryError("A page could not be saved", cause)
    class DatabaseWrite(cause: Throwable) : RepositoryError("The document could not be saved", cause)
    class Cleanup(cause: Throwable? = null) : RepositoryError("Some local files could not be cleaned up", cause)
    class InvalidFolderName : RepositoryError("Choose a folder name")
    class DuplicateFolderName : RepositoryError("A folder with this name already exists")
    class FolderNotFound : RepositoryError("This folder is no longer available")
}

data class NewPage(
    val original: () -> InputStream,
    val processed: () -> InputStream,
    val thumbnail: () -> InputStream,
    val originalExtension: String = "jpg",
    val processedExtension: String = "jpg",
    val thumbnailExtension: String = "jpg",
    val width: Int,
    val height: Int
)

interface DocumentRepository {
    fun observeFolders(): Flow<List<Folder>>
    fun observeDocuments(): Flow<List<Document>>
    fun observeTrash(): Flow<List<Document>>
    fun observeDocument(id: DocumentId): Flow<Document?>
    suspend fun create(name: String, pages: List<NewPage>, folderId: FolderId? = null): DocumentId
    suspend fun rename(id: DocumentId, name: String)
    suspend fun setFavorite(id: DocumentId, favorite: Boolean)
    suspend fun moveToFolder(id: DocumentId, folderId: FolderId?)
    suspend fun moveToTrash(id: DocumentId)
    suspend fun restore(id: DocumentId)
    suspend fun permanentlyDelete(id: DocumentId)
    suspend fun createFolder(name: String): FolderId
    suspend fun renameFolder(id: FolderId, name: String)
    suspend fun deleteFolder(id: FolderId)
    suspend fun bulkMoveToTrash(ids: List<DocumentId>)
    suspend fun bulkMoveToFolder(ids: List<DocumentId>, folderId: FolderId?)
    suspend fun bulkSetFavorite(ids: List<DocumentId>, favorite: Boolean)
    suspend fun restoreMultiple(ids: List<DocumentId>)
    suspend fun permanentlyDeleteMultiple(ids: List<DocumentId>)
    suspend fun emptyTrash()
}

class LocalDocumentRepository(
    private val database: OScanDatabase,
    private val fileStore: DocumentFileStore,
    private val clock: Clock = Clock.systemUTC(),
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : DocumentRepository {
    private val dao: LibraryDao = database.libraryDao()

    override fun observeFolders(): Flow<List<Folder>> = dao.observeFolders().map { folders ->
        folders.map {
            Folder(
                id = FolderId(it.id),
                name = it.name,
                createdAt = Instant.ofEpochMilli(it.createdAtEpochMillis),
                modifiedAt = Instant.ofEpochMilli(it.modifiedAtEpochMillis)
            )
        }
    }

    override fun observeDocuments(): Flow<List<Document>> =
        dao.observeActiveDocuments().map { documents -> documents.map { it.toModel() } }

    override fun observeTrash(): Flow<List<Document>> =
        dao.observeTrash().map { documents -> documents.map { it.toModel() } }

    override fun observeDocument(id: DocumentId): Flow<Document?> =
        dao.observeDocument(id.value).map { it?.toModel() }

    override suspend fun create(name: String, pages: List<NewPage>, folderId: FolderId?): DocumentId {
        val safeName = name.trim()
        if (safeName.isEmpty()) throw RepositoryError.InvalidName()
        if (pages.isEmpty()) throw RepositoryError.EmptyDocument()

        val documentId = newId()
        val now = clock.millis()
        val pageEntities = mutableListOf<PageEntity>()
        try {
            pages.forEachIndexed { position, page ->
                require(page.width > 0 && page.height > 0)
                val pageId = newId()
                val original = page.original().use {
                    fileStore.write(documentId, pageId, AssetKind.ORIGINAL, page.originalExtension, it)
                }
                val processed = page.processed().use {
                    fileStore.write(documentId, pageId, AssetKind.PROCESSED, page.processedExtension, it)
                }
                val thumbnail = page.thumbnail().use {
                    fileStore.write(documentId, pageId, AssetKind.THUMBNAIL, page.thumbnailExtension, it)
                }
                pageEntities += PageEntity(
                    id = pageId,
                    documentId = documentId,
                    position = position,
                    originalAsset = original,
                    processedAsset = processed,
                    thumbnailAsset = thumbnail,
                    width = page.width,
                    height = page.height
                )
            }
        } catch (error: Throwable) {
            fileStore.deleteDocument(documentId)
            throw RepositoryError.AssetWrite(error)
        }

        try {
            dao.insertDocumentGraph(
                document = DocumentEntity(documentId, safeName, createdAtEpochMillis = now, modifiedAtEpochMillis = now),
                pages = pageEntities,
                folderId = folderId?.value
            )
        } catch (error: Throwable) {
            fileStore.deleteDocument(documentId)
            throw RepositoryError.DatabaseWrite(error)
        }
        return DocumentId(documentId)
    }

    override suspend fun rename(id: DocumentId, name: String) {
        val safeName = name.trim()
        if (safeName.isEmpty()) throw RepositoryError.InvalidName()
        if (dao.rename(id.value, safeName, clock.millis()) == 0) throw RepositoryError.NotFound()
    }

    override suspend fun setFavorite(id: DocumentId, favorite: Boolean) {
        if (dao.setFavorite(id.value, favorite, clock.millis()) == 0) throw RepositoryError.NotFound()
    }

    override suspend fun moveToFolder(id: DocumentId, folderId: FolderId?) {
        if (dao.getDocument(id.value) == null) throw RepositoryError.NotFound()
        if (folderId != null && dao.getFolder(folderId.value) == null) throw RepositoryError.NotFound()
        database.withTransaction {
            dao.clearDocumentFolder(id.value)
            if (folderId != null) {
                dao.setDocumentFolder(DocumentFolderEntity(id.value, folderId.value))
            }
            dao.touchDocument(id.value, clock.millis())
        }
    }

    override suspend fun moveToTrash(id: DocumentId) {
        val document = dao.getDocument(id.value) ?: throw RepositoryError.NotFound()
        val previousFolder = document.folders.singleOrNull()?.id
        database.withTransaction {
            dao.markTrashed(id.value, clock.millis(), previousFolder)
            dao.clearDocumentFolder(id.value)
        }
    }

    override suspend fun restore(id: DocumentId) {
        val document = dao.getDocument(id.value) ?: throw RepositoryError.NotFound()
        val previousFolder = document.document.previousFolderId
        database.withTransaction {
            dao.markRestored(id.value, clock.millis())
            if (previousFolder != null && dao.getFolder(previousFolder) != null) {
                dao.setDocumentFolder(DocumentFolderEntity(id.value, previousFolder))
            }
        }
    }

    override suspend fun permanentlyDelete(id: DocumentId) {
        val document = dao.getDocument(id.value) ?: throw RepositoryError.NotFound()
        database.withTransaction { dao.deleteDocument(document.document) }
        if (!fileStore.deleteDocument(id.value)) throw RepositoryError.Cleanup()
    }

    override suspend fun createFolder(name: String): FolderId {
        val safeName = name.trim()
        if (safeName.isEmpty()) throw RepositoryError.InvalidFolderName()
        if (dao.folderNameExists(safeName)) throw RepositoryError.DuplicateFolderName()
        val folderId = newId()
        val now = clock.millis()
        dao.insertFolder(
            com.oscan.android.data.db.FolderEntity(
                id = folderId,
                name = safeName,
                createdAtEpochMillis = now,
                modifiedAtEpochMillis = now
            )
        )
        return FolderId(folderId)
    }

    override suspend fun renameFolder(id: FolderId, name: String) {
        val safeName = name.trim()
        if (safeName.isEmpty()) throw RepositoryError.InvalidFolderName()
        if (dao.folderNameExistsExcluding(safeName, id.value)) throw RepositoryError.DuplicateFolderName()
        if (dao.renameFolder(id.value, safeName, clock.millis()) == 0) throw RepositoryError.FolderNotFound()
    }

    override suspend fun deleteFolder(id: FolderId) {
        if (dao.getFolder(id.value) == null) throw RepositoryError.FolderNotFound()
        dao.deleteFolder(id.value)
    }

    override suspend fun bulkMoveToTrash(ids: List<DocumentId>) {
        ids.forEach { moveToTrash(it) }
    }

    override suspend fun bulkMoveToFolder(ids: List<DocumentId>, folderId: FolderId?) {
        ids.forEach { moveToFolder(it, folderId) }
    }

    override suspend fun bulkSetFavorite(ids: List<DocumentId>, favorite: Boolean) {
        ids.forEach { setFavorite(it, favorite) }
    }

    override suspend fun restoreMultiple(ids: List<DocumentId>) {
        ids.forEach { restore(it) }
    }

    override suspend fun permanentlyDeleteMultiple(ids: List<DocumentId>) {
        ids.forEach { permanentlyDelete(it) }
    }

    override suspend fun emptyTrash() {
        val trashedDocs = dao.observeTrash().first()
        trashedDocs.forEach { document ->
            database.withTransaction { dao.deleteDocument(document.document) }
            fileStore.deleteDocument(document.document.id)
        }
    }

    private fun DocumentAggregate.toModel(): Document {
        val folder = folders.singleOrNull()?.let {
            Folder(FolderId(it.id), it.name, Instant.ofEpochMilli(it.createdAtEpochMillis), Instant.ofEpochMilli(it.modifiedAtEpochMillis))
        }
        return Document(
            id = DocumentId(document.id),
            name = document.name,
            pages = pages.sortedBy { it.position }.map {
                Page(
                    PageId(it.id), DocumentId(it.documentId), it.position,
                    it.originalAsset, it.processedAsset, it.thumbnailAsset,
                    it.width, it.height, it.rotationDegrees
                )
            },
            folder = folder,
            isFavorite = document.isFavorite,
            createdAt = Instant.ofEpochMilli(document.createdAtEpochMillis),
            modifiedAt = Instant.ofEpochMilli(document.modifiedAtEpochMillis),
            trashedAt = document.trashedAtEpochMillis?.let(Instant::ofEpochMilli)
        )
    }
}

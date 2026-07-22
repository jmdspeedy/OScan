package com.oscan.android.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Transaction
    @Query("SELECT * FROM documents WHERE trashedAtEpochMillis IS NULL ORDER BY modifiedAtEpochMillis DESC")
    fun observeActiveDocuments(): Flow<List<DocumentAggregate>>

    @Transaction
    @Query("SELECT * FROM documents WHERE trashedAtEpochMillis IS NOT NULL ORDER BY trashedAtEpochMillis DESC")
    fun observeTrash(): Flow<List<DocumentAggregate>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun observeDocument(id: String): Flow<DocumentAggregate?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocument(id: String): DocumentAggregate?

    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    @Insert
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDocumentFolder(membership: DocumentFolderEntity)

    @Query("DELETE FROM document_folders WHERE documentId = :documentId")
    suspend fun clearDocumentFolder(documentId: String)

    @Query("UPDATE documents SET name = :name, modifiedAtEpochMillis = :modifiedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, modifiedAt: Long): Int

    @Query("UPDATE documents SET isFavorite = :favorite, modifiedAtEpochMillis = :modifiedAt WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, modifiedAt: Long): Int

    @Query("UPDATE documents SET modifiedAtEpochMillis = :modifiedAt WHERE id = :id")
    suspend fun touchDocument(id: String, modifiedAt: Long): Int

    @Query("UPDATE documents SET trashedAtEpochMillis = :trashedAt, previousFolderId = :previousFolderId, modifiedAtEpochMillis = :trashedAt WHERE id = :id")
    suspend fun markTrashed(id: String, trashedAt: Long, previousFolderId: String?): Int

    @Query("UPDATE documents SET trashedAtEpochMillis = NULL, previousFolderId = NULL, modifiedAtEpochMillis = :restoredAt WHERE id = :id")
    suspend fun markRestored(id: String, restoredAt: Long): Int

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolder(id: String): FolderEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE LOWER(name) = LOWER(:name))")
    suspend fun folderNameExists(name: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE LOWER(name) = LOWER(:name) AND id != :excludeId)")
    suspend fun folderNameExistsExcluding(name: String, excludeId: String): Boolean

    @Query("UPDATE folders SET name = :name, modifiedAtEpochMillis = :modifiedAt WHERE id = :id")
    suspend fun renameFolder(id: String, name: String, modifiedAt: Long): Int

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String): Int

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("SELECT * FROM pages WHERE id = :id LIMIT 1")
    suspend fun getPage(id: String): PageEntity?

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    suspend fun getPagesForDocument(documentId: String): List<PageEntity>

    @Query("UPDATE pages SET position = :position WHERE id = :id")
    suspend fun updatePagePosition(id: String, position: Int)

    @Query("UPDATE pages SET rotationDegrees = :rotationDegrees, width = :width, height = :height WHERE id = :id")
    suspend fun updatePageRotation(id: String, rotationDegrees: Int, width: Int, height: Int)

    @Query("UPDATE pages SET width = :width, height = :height WHERE id = :id")
    suspend fun updatePageDimensions(id: String, width: Int, height: Int)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePage(id: String)

    @Transaction
    suspend fun insertDocumentGraph(
        document: DocumentEntity,
        pages: List<PageEntity>,
        folderId: String?
    ) {
        insertDocument(document)
        insertPages(pages)
        if (folderId != null) setDocumentFolder(DocumentFolderEntity(document.id, folderId))
    }
}

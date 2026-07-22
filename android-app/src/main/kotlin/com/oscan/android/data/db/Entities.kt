package com.oscan.android.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isFavorite: Boolean = false,
    val createdAtEpochMillis: Long,
    val modifiedAtEpochMillis: Long,
    val trashedAtEpochMillis: Long? = null,
    val previousFolderId: String? = null
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId"), Index(value = ["documentId", "position"], unique = true)]
)
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val position: Int,
    val originalAsset: String,
    val processedAsset: String,
    val thumbnailAsset: String,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
    val cropCorners: String? = null
)

@Entity(
    tableName = "folders",
    indices = [Index(value = ["name"], unique = true)]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val modifiedAtEpochMillis: Long
)

@Entity(
    tableName = "document_folders",
    primaryKeys = ["documentId", "folderId"],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"], unique = true), Index("folderId")]
)
data class DocumentFolderEntity(
    val documentId: String,
    val folderId: String
)

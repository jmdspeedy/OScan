package com.oscan.android.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DocumentAggregate(
    @Embedded val document: DocumentEntity,
    @Relation(parentColumn = "id", entityColumn = "documentId")
    val pages: List<PageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = DocumentFolderEntity::class,
            parentColumn = "documentId",
            entityColumn = "folderId"
        )
    )
    val folders: List<FolderEntity>
)

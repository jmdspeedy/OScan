package com.oscan.android.data.model

import java.time.Instant

@JvmInline
value class DocumentId(val value: String)

@JvmInline
value class PageId(val value: String)

@JvmInline
value class FolderId(val value: String)

data class Document(
    val id: DocumentId,
    val name: String,
    val pages: List<Page>,
    val folder: Folder?,
    val isFavorite: Boolean,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val trashedAt: Instant?
)

data class Page(
    val id: PageId,
    val documentId: DocumentId,
    val position: Int,
    val originalAsset: String,
    val processedAsset: String,
    val thumbnailAsset: String,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int
)

data class Folder(
    val id: FolderId,
    val name: String,
    val createdAt: Instant,
    val modifiedAt: Instant
)

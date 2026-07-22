package com.oscan.android.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.oscan.android.data.db.OScanDatabase
import com.oscan.android.data.db.FolderEntity
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.storage.DocumentFileStore
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalDocumentRepositoryTest {
    private lateinit var database: OScanDatabase
    private lateinit var repository: LocalDocumentRepository
    private lateinit var fileStore: DocumentFileStore
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, OScanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fileStore = DocumentFileStore(context)
        var nextId = 0
        repository = LocalDocumentRepository(
            database = database,
            fileStore = fileStore,
            clock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC),
            newId = { "id_${nextId++}" }
        )
    }

    @After
    fun tearDown() {
        database.close()
        fileStore.deleteDocument("id_0")
    }

    @Test
    fun createPersistsOrderedPagesAndAssets() = runTest {
        val id = repository.create(" Receipt ", listOf(page("one"), page("two")))

        val saved = repository.observeDocument(id).first()!!
        assertEquals("Receipt", saved.name)
        assertEquals(listOf(0, 1), saved.pages.map { it.position })
        saved.pages.forEach { page ->
            assertTrue(fileStore.resolve(page.originalAsset).exists())
            assertTrue(fileStore.resolve(page.processedAsset).exists())
            assertTrue(fileStore.resolve(page.thumbnailAsset).exists())
        }
    }

    @Test
    fun renameFavoriteTrashAndRestoreAreObservable() = runTest {
        val id = repository.create("First", listOf(page("page")))
        repository.rename(id, "Second")
        repository.setFavorite(id, true)
        repository.moveToTrash(id)

        assertTrue(repository.observeDocuments().first().isEmpty())
        assertEquals("Second", repository.observeTrash().first().single().name)

        repository.restore(id)
        val restored = repository.observeDocuments().first().single()
        assertTrue(restored.isFavorite)
        assertEquals(null, restored.trashedAt)
    }

    @Test
    fun failedAssetWriteRollsBackFilesAndDatabase() = runTest {
        val broken = page("ok").copy(processed = { error("broken stream") })

        assertFailsWith<RepositoryError.AssetWrite> {
            repository.create("Broken", listOf(broken))
        }
        assertTrue(repository.observeDocuments().first().isEmpty())
        assertFalse(fileStore.resolve("id_0/originals/id_1.jpg").exists())
    }

    @Test
    fun permanentDeleteRemovesDatabaseRecordAndAssets() = runTest {
        val id = repository.create("Delete me", listOf(page("page")))
        val asset = repository.observeDocument(id).first()!!.pages.single().originalAsset

        repository.permanentlyDelete(DocumentId(id.value))

        assertEquals(null, repository.observeDocument(id).first())
        assertFalse(fileStore.resolve(asset).exists())
    }

    @Test
    fun moveToFolderAndBackToUnfiledUpdatesDocument() = runTest {
        val id = repository.create("Filed", listOf(page("page")))
        database.libraryDao().insertFolder(
            FolderEntity("folder_1", "Receipts", 1L, 1L)
        )

        repository.moveToFolder(id, com.oscan.android.data.model.FolderId("folder_1"))
        assertEquals("Receipts", repository.observeDocument(id).first()!!.folder?.name)

        repository.moveToFolder(id, null)
        assertEquals(null, repository.observeDocument(id).first()!!.folder)
    }

    @Test
    fun createRenameAndDeleteFolderMovesDocumentsToUnfiled() = runTest {
        val folderId = repository.createFolder("Tax 2026")
        assertEquals("Tax 2026", repository.observeFolders().first().single().name)

        val docId = repository.create("W2 Form", listOf(page("tax")), folderId)
        assertEquals("Tax 2026", repository.observeDocument(docId).first()!!.folder?.name)

        repository.renameFolder(folderId, "Taxes 2026")
        assertEquals("Taxes 2026", repository.observeDocument(docId).first()!!.folder?.name)

        // Deleting folder moves document to Unfiled (folder == null)
        repository.deleteFolder(folderId)
        assertTrue(repository.observeFolders().first().isEmpty())
        val unfiledDoc = repository.observeDocument(docId).first()!!
        assertEquals(null, unfiledDoc.folder)
    }

    @Test
    fun duplicateOrEmptyFolderNameThrowsError() = runTest {
        repository.createFolder("Invoices")
        assertFailsWith<RepositoryError.InvalidFolderName> { repository.createFolder("   ") }
        assertFailsWith<RepositoryError.DuplicateFolderName> { repository.createFolder("invoices") }
    }

    @Test
    fun emptyTrashDeletesAllTrashedDocumentsAndFiles() = runTest {
        val doc1 = repository.create("Doc 1", listOf(page("1")))
        val doc2 = repository.create("Doc 2", listOf(page("2")))
        val asset1 = repository.observeDocument(doc1).first()!!.pages.single().originalAsset
        val asset2 = repository.observeDocument(doc2).first()!!.pages.single().originalAsset

        repository.moveToTrash(doc1)
        repository.moveToTrash(doc2)
        assertEquals(2, repository.observeTrash().first().size)

        repository.emptyTrash()
        assertTrue(repository.observeTrash().first().isEmpty())
        assertFalse(fileStore.resolve(asset1).exists())
        assertFalse(fileStore.resolve(asset2).exists())
    }

    @Test
    fun bulkOperationsMoveAndFavoriteDocuments() = runTest {
        val folderId = repository.createFolder("Work")
        val doc1 = repository.create("Doc A", listOf(page("a")))
        val doc2 = repository.create("Doc B", listOf(page("b")))

        repository.bulkSetFavorite(listOf(doc1, doc2), true)
        assertTrue(repository.observeDocument(doc1).first()!!.isFavorite)
        assertTrue(repository.observeDocument(doc2).first()!!.isFavorite)

        repository.bulkMoveToFolder(listOf(doc1, doc2), folderId)
        assertEquals("Work", repository.observeDocument(doc1).first()!!.folder?.name)
        assertEquals("Work", repository.observeDocument(doc2).first()!!.folder?.name)

        repository.bulkMoveToTrash(listOf(doc1, doc2))
        assertTrue(repository.observeDocuments().first().isEmpty())
        assertEquals(2, repository.observeTrash().first().size)
    }

    @Test
    fun addPagesAppendsNewPagesToExistingDocument() = runTest {
        val id = repository.create("Doc", listOf(page("p1")))
        repository.addPages(id, listOf(page("p2"), page("p3")))

        val doc = repository.observeDocument(id).first()!!
        assertEquals(listOf(0, 1, 2), doc.pages.map { it.position })
        assertEquals(3, doc.pages.size)
    }

    @Test
    fun reorderPagesUpdatesPositionsTransactionally() = runTest {
        val id = repository.create("Doc", listOf(page("p1"), page("p2"), page("p3")))
        val doc = repository.observeDocument(id).first()!!
        val pageIds = doc.pages.map { it.id }

        // Reorder to [p3, p1, p2]
        repository.reorderPages(id, listOf(pageIds[2], pageIds[0], pageIds[1]))

        val updated = repository.observeDocument(id).first()!!
        assertEquals(listOf(pageIds[2], pageIds[0], pageIds[1]), updated.pages.map { it.id })
        assertEquals(listOf(0, 1, 2), updated.pages.map { it.position })
    }

    @Test
    fun rotatePageUpdatesRotationDegreesAndDimensions() = runTest {
        val id = repository.create("Doc", listOf(page("p1")))
        val pageId = repository.observeDocument(id).first()!!.pages.single().id

        repository.rotatePage(id, pageId, 90)
        val rotated = repository.observeDocument(id).first()!!.pages.single()
        assertEquals(90, rotated.rotationDegrees)
        assertEquals(200, rotated.width)
        assertEquals(100, rotated.height)

        repository.rotatePage(id, pageId, 90)
        val rotated180 = repository.observeDocument(id).first()!!.pages.single()
        assertEquals(180, rotated180.rotationDegrees)
        assertEquals(100, rotated180.width)
        assertEquals(200, rotated180.height)
    }

    @Test
    fun deletePageRemovesPageAssetsAndReindexesRemainingPages() = runTest {
        val id = repository.create("Doc", listOf(page("p1"), page("p2"), page("p3")))
        val pages = repository.observeDocument(id).first()!!.pages
        val page1Asset = pages[0].originalAsset

        repository.deletePage(id, pages[0].id)

        val updated = repository.observeDocument(id).first()!!
        assertEquals(2, updated.pages.size)
        assertEquals(listOf(0, 1), updated.pages.map { it.position })
        assertFalse(fileStore.resolve(page1Asset).exists())
    }

    @Test
    fun deleteLastPageMovesDocumentToTrash() = runTest {
        val id = repository.create("Single", listOf(page("p1")))
        val pageId = repository.observeDocument(id).first()!!.pages.single().id

        repository.deletePage(id, pageId)

        assertTrue(repository.observeDocuments().first().isEmpty())
        assertEquals("Single", repository.observeTrash().first().single().name)
    }

    private fun page(value: String) = NewPage(
        original = { ByteArrayInputStream("original-$value".toByteArray()) },
        processed = { ByteArrayInputStream("processed-$value".toByteArray()) },
        thumbnail = { ByteArrayInputStream("thumbnail-$value".toByteArray()) },
        width = 100,
        height = 200
    )
}

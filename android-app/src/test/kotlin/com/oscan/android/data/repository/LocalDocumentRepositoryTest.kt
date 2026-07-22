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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
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

    private fun page(value: String) = NewPage(
        original = { ByteArrayInputStream("original-$value".toByteArray()) },
        processed = { ByteArrayInputStream("processed-$value".toByteArray()) },
        thumbnail = { ByteArrayInputStream("thumbnail-$value".toByteArray()) },
        width = 100,
        height = 200
    )
}

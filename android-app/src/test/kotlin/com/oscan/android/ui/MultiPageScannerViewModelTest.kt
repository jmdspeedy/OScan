package com.oscan.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.NewPage
import com.oscan.android.data.session.ScanSessionStore
import com.oscan.android.data.session.SessionPageStatus
import com.oscan.android.engine.DetectionResult
import com.oscan.android.engine.ScannerEngine
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.opencv.core.Point
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MultiPageScannerViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var store: ScanSessionStore

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        val ids = ArrayDeque(listOf("vm_session", "vm_page_1", "vm_page_2"))
        store = ScanSessionStore(context) { ids.removeFirst() }
        store.loadActive()?.let { store.discard(it.id) }
    }

    @AfterTest
    fun tearDown() {
        store.loadActive()?.let { store.discard(it.id) }
        Dispatchers.resetMain()
    }

    @Test
    fun failedImportDoesNotBlockValidPageReviewAndOrderIsPreserved() {
        val good = File(context.cacheDir, "good-import.jpg").apply { writeText("good") }
        val bad = File(context.cacheDir, "bad-import.jpg").apply { writeText("bad") }
        val viewModel = ScannerViewModel(
            scannerEngine = FakeScannerEngine(),
            repository = EmptyRepository,
            sessionStore = store,
            contentResolver = context.contentResolver
        )

        viewModel.onImagesSelected(listOf(Uri.fromFile(good), Uri.fromFile(bad)))

        val state = assertIs<ScannerUiState.CropReady>(viewModel.uiState.value)
        assertEquals(2, state.session.pages.size)
        assertEquals(listOf(0, 1), state.session.pages.map { it.position })
        assertEquals(SessionPageStatus.CROP_REVIEW, state.session.pages[0].status)
        assertEquals(SessionPageStatus.FAILED, state.session.pages[1].status)
    }

    private class FakeScannerEngine : ScannerEngine {
        override suspend fun decodeAndDetect(uri: Uri): DetectionResult {
            if (File(requireNotNull(uri.path)).readText() == "bad") error("decode failed")
            val bitmap = Bitmap.createBitmap(100, 140, Bitmap.Config.ARGB_8888)
            val dimensions = ImageDimensions(100, 140)
            return DetectionResult(
                sourceDimensions = dimensions,
                corners = CornerPoints(
                    Point(5.0, 5.0), Point(95.0, 5.0), Point(95.0, 135.0), Point(5.0, 135.0)
                ),
                isAutoDetected = true,
                previewBitmap = bitmap
            )
        }

        override suspend fun cropAndFilter(uri: Uri, corners: CornerPoints, filterType: FilterType): Bitmap =
            Bitmap.createBitmap(90, 130, Bitmap.Config.ARGB_8888)
    }

    private object EmptyRepository : DocumentRepository {
        override fun observeFolders(): Flow<List<Folder>> = flowOf(emptyList())
        override fun observeDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override fun observeTrash(): Flow<List<Document>> = flowOf(emptyList())
        override fun observeDocument(id: DocumentId): Flow<Document?> = flowOf(null)
        override suspend fun create(name: String, pages: List<NewPage>, folderId: FolderId?): DocumentId = DocumentId("doc")
        override suspend fun rename(id: DocumentId, name: String) = Unit
        override suspend fun setFavorite(id: DocumentId, favorite: Boolean) = Unit
        override suspend fun moveToFolder(id: DocumentId, folderId: FolderId?) = Unit
        override suspend fun moveToTrash(id: DocumentId) = Unit
        override suspend fun restore(id: DocumentId) = Unit
        override suspend fun permanentlyDelete(id: DocumentId) = Unit
        override suspend fun createFolder(name: String): FolderId = FolderId("folder")
        override suspend fun renameFolder(id: FolderId, name: String) = Unit
        override suspend fun deleteFolder(id: FolderId) = Unit
        override suspend fun bulkMoveToTrash(ids: List<DocumentId>) = Unit
        override suspend fun bulkMoveToFolder(ids: List<DocumentId>, folderId: FolderId?) = Unit
        override suspend fun bulkSetFavorite(ids: List<DocumentId>, favorite: Boolean) = Unit
        override suspend fun restoreMultiple(ids: List<DocumentId>) = Unit
        override suspend fun permanentlyDeleteMultiple(ids: List<DocumentId>) = Unit
        override suspend fun emptyTrash() = Unit
        override suspend fun addPages(id: DocumentId, pages: List<NewPage>): List<com.oscan.android.data.model.PageId> =
            pages.mapIndexed { index, _ -> com.oscan.android.data.model.PageId("page_$index") }
        override suspend fun reorderPages(id: DocumentId, pageIdsInOrder: List<com.oscan.android.data.model.PageId>) = Unit
        override suspend fun rotatePage(id: DocumentId, pageId: com.oscan.android.data.model.PageId, deltaDegrees: Int) = Unit
        override suspend fun updatePageAssets(
            id: DocumentId,
            pageId: com.oscan.android.data.model.PageId,
            processedStream: () -> java.io.InputStream,
            thumbnailStream: () -> java.io.InputStream,
            width: Int,
            height: Int
        ) = Unit
        override suspend fun deletePage(id: DocumentId, pageId: com.oscan.android.data.model.PageId) = Unit
    }
}

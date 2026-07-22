package com.oscan.android.data.session

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.opencv.core.Point
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScanSessionStoreTest {
    private lateinit var store: ScanSessionStore
    private val ids = ArrayDeque(listOf("session_test", "page_one"))

    @BeforeTest
    fun setUp() {
        store = ScanSessionStore(ApplicationProvider.getApplicationContext()) { ids.removeFirst() }
        store.loadActive()?.let { store.discard(it.id) }
    }

    @AfterTest
    fun tearDown() {
        store.loadActive()?.let { store.discard(it.id) }
    }

    @Test
    fun acceptedPageMetadataAndAssetsRecoverFromActiveSession() {
        val session = store.create()
        val pageId = store.newPageId()
        val sourcePath = store.importSource(session.id, pageId, "image".byteInputStream())
        val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
        val processedPath = store.writeProcessed(session.id, pageId, bitmap)
        val thumbnailPath = store.writeThumbnail(session.id, pageId, bitmap)
        val corners = CornerPoints(
            Point(1.0, 2.0), Point(30.0, 2.0), Point(30.0, 46.0), Point(1.0, 46.0)
        )
        store.save(
            session.copy(
                currentPageId = pageId,
                documentName = "Receipts",
                pages = listOf(
                    SessionPage(
                        id = pageId,
                        position = 0,
                        status = SessionPageStatus.ACCEPTED,
                        sourcePath = sourcePath,
                        processedPath = processedPath,
                        thumbnailPath = thumbnailPath,
                        sourceWidth = 32,
                        sourceHeight = 48,
                        outputWidth = 32,
                        outputHeight = 48,
                        corners = corners,
                        initialCorners = corners,
                        filter = FilterType.MAGIC
                    )
                )
            )
        )

        val restored = assertNotNull(store.loadActive())
        assertEquals("Receipts", restored.documentName)
        assertEquals(pageId, restored.currentPageId)
        assertEquals(SessionPageStatus.ACCEPTED, restored.pages.single().status)
        assertEquals(FilterType.MAGIC, restored.pages.single().filter)
        assertEquals(30.0, restored.pages.single().corners?.topRight?.x)
        assertTrue(store.resolve(processedPath).isFile)
        bitmap.recycle()
    }

    @Test
    fun missingAcceptedOutputReturnsPageToCropReview() {
        val session = store.create()
        val pageId = store.newPageId()
        val sourcePath = store.importSource(session.id, pageId, "image".byteInputStream())
        store.save(
            session.copy(
                pages = listOf(
                    SessionPage(
                        id = pageId,
                        position = 0,
                        status = SessionPageStatus.ACCEPTED,
                        sourcePath = sourcePath,
                        processedPath = "session_test/pages/page_one/missing.jpg"
                    )
                )
            )
        )

        val restored = assertNotNull(store.loadActive())
        assertEquals(SessionPageStatus.CROP_REVIEW, restored.pages.single().status)
        assertNull(restored.pages.single().processedPath)
    }

    @Test
    fun discardRemovesDraftAndActiveMarker() {
        val session = store.create()
        val sessionDirectory = File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "scan-sessions/${session.id}")

        assertTrue(store.discard(session.id))

        assertFalse(sessionDirectory.exists())
        assertNull(store.loadActive())
    }
}

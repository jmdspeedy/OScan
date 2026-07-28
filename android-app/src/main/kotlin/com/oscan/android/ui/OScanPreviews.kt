package com.oscan.android.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.Folder
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.model.Page
import com.oscan.android.data.model.PageId
import com.oscan.android.data.preferences.AccentTheme
import com.oscan.android.data.preferences.AppLanguage
import com.oscan.android.data.preferences.LibraryPresentation
import com.oscan.android.data.preferences.ThemeChoice
import com.oscan.android.data.preferences.UserPreferences
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.data.session.ScanSession
import com.oscan.android.data.session.SessionPage
import com.oscan.android.data.session.SessionPageStatus
import com.oscan.android.data.vault.VaultDocument
import com.oscan.android.data.vault.VaultSessionState
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.android.ui.vault.VaultBiometricSetupScreen
import com.oscan.android.ui.vault.VaultSetupScreen
import com.oscan.android.ui.vault.VaultTrashScreen
import com.oscan.android.ui.vault.VaultUnlockScreen
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point
import java.time.Instant

@Preview(name = "Phone · Light", group = "OScan pages", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Phone · Dark", group = "OScan pages", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class OScanPagePreview

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    OScanTheme(content = content)
}

private val previewFolder = Folder(FolderId("preview-folder"), "Receipts", Instant.now(), Instant.now())
private val previewDocumentId = DocumentId("preview-document")
private val previewPage = Page(
    id = PageId("preview-page"),
    documentId = previewDocumentId,
    position = 0,
    originalAsset = "preview/original.jpg",
    processedAsset = "preview/processed.jpg",
    thumbnailAsset = "preview/thumbnail.jpg",
    width = 1200,
    height = 1600,
    rotationDegrees = 0
)
private val previewDocument = Document(
    id = previewDocumentId,
    name = "July expense report",
    pages = listOf(previewPage),
    folder = previewFolder,
    isFavorite = true,
    createdAt = Instant.now(),
    modifiedAt = Instant.now(),
    trashedAt = null
)

@Composable
private fun previewFileStore() = DocumentFileStore(LocalContext.current)

@OScanPagePreview
@Composable
fun WelcomePagePreview() = PreviewTheme { StartScreen {} }

@OScanPagePreview
@Composable
fun CameraPagePreview() = PreviewTheme { CameraTransitionPreview() }

@OScanPagePreview
@Composable
fun CropPagePreview() {
    val bitmap = remember { Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888) }
    val dimensions = ImageDimensions(900, 1200)
    val corners = CornerPoints(Point(70.0, 90.0), Point(830.0, 80.0), Point(840.0, 1120.0), Point(60.0, 1130.0))
    PreviewTheme { CropScreen(bitmap, dimensions, corners, true) { _, _, _ -> } }
}

@OScanPagePreview
@Composable
fun TreatmentPagePreview() {
    val bitmap = remember { Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888) }
    PreviewTheme { PreviewScreen(bitmap, FilterType.MAGIC, {}, onExportPdfRequested = {}) }
}

@OScanPagePreview
@Composable
fun IdCardAdjustmentPagePreview() {
    val bitmap = remember { Bitmap.createBitmap(900, 560, Bitmap.Config.ARGB_8888) }
    val corners = CornerPoints(Point(40.0, 40.0), Point(860.0, 40.0), Point(860.0, 520.0), Point(40.0, 520.0))
    val front = SessionPage("front", 0, SessionPageStatus.CROP_REVIEW, sourceWidth = 900, sourceHeight = 560)
    val back = SessionPage("back", 1, SessionPageStatus.CROP_REVIEW, sourceWidth = 900, sourceHeight = 560)
    PreviewTheme {
        IdCardAdjustmentScreen(
            ScannerUiState.IdCardAdjust(
                ScanSession("id-card-preview", listOf(front, back)), front, back, bitmap, bitmap,
                corners, corners, corners, corners, true, true
            )
        ) { _, _, _, _ -> }
    }
}

@OScanPagePreview
@Composable
fun ExportSuccessPagePreview() = PreviewTheme { ExportSuccessScreen(Uri.EMPTY, {}, {}) }

@OScanPagePreview
@Composable
fun LibraryPagePreview() = PreviewTheme {
    HomeLibraryScreen(
        state = LibraryUiState(isLoading = false, documents = listOf(previewDocument), recentDocuments = listOf(previewDocument), folders = listOf(previewFolder)),
        fileStore = previewFileStore(),
        gridState = rememberLazyGridState(),
        listState = rememberLazyListState(),
        onOpenDocument = {}, onSearchQueryChange = {}, onFilterChange = {}, onToggleSelectionMode = {},
        onToggleDocumentSelection = {}, onOpenFolder = {}, onCreateFolderRequested = {}, emptyContent = {}
    )
}

@OScanPagePreview
@Composable
fun FoldersPagePreview() = PreviewTheme {
    FolderOverviewScreen(listOf(previewFolder), listOf(previewDocument), {}, {}, { _, _ -> }, {}, {})
}

@OScanPagePreview
@Composable
fun FolderDetailPagePreview() = PreviewTheme {
    FolderDetailScreen(
        previewFolder, listOf(previewDocument), previewFileStore(), rememberLazyGridState(), rememberLazyListState(),
        LibraryPresentation.GRID, false, emptySet(), {}, {}, {}, {}, {}, {}
    )
}

@OScanPagePreview
@Composable
fun TrashPagePreview() = PreviewTheme {
    TrashScreen(listOf(previewDocument.copy(trashedAt = Instant.now())), previewFileStore(), false, emptySet(), {}, {}, {}, {}, {}, {}, {}, {})
}

@OScanPagePreview
@Composable
fun DocumentDetailsPagePreview() = PreviewTheme {
    DocumentDetailScreen(
        document = previewDocument, requestedDocumentExists = true, folders = listOf(previewFolder),
        fileStore = previewFileStore(), snackbarHostState = remember { SnackbarHostState() }, onBack = {},
        onRename = {}, onFavorite = {}, onMove = {}, onTrash = {}, onOpenPage = {}
    )
}

@OScanPagePreview
@Composable
fun PageViewerPagePreview() = PreviewTheme { PageViewerScreen(previewDocument, 0, previewFileStore(), {}) }

@OScanPagePreview
@Composable
fun CaptureSettingsPagePreview() = PreviewTheme { CaptureSettingsScreen(UserPreferences(), {}, {}, {}) }

@OScanPagePreview
@Composable
fun EnhancementSettingsPagePreview() = PreviewTheme { EnhancementSettingsScreen(UserPreferences(), {}, {}) }

@OScanPagePreview
@Composable
fun AppearanceSettingsPagePreview() = PreviewTheme { AppearanceSettingsScreen(ThemeChoice.SYSTEM, AccentTheme.TEAL, {}, {}, {}) }

@OScanPagePreview
@Composable
fun LanguageSettingsPagePreview() = PreviewTheme { LanguageSettingsScreen(AppLanguage.SYSTEM, {}, {}) }

@OScanPagePreview
@Composable
fun StorageSettingsPagePreview() = PreviewTheme { StorageSettingsScreen(previewFileStore(), {}, {}) }

@OScanPagePreview
@Composable
fun PrivacyPagePreview() = PreviewTheme { PrivacyScreen {} }

@OScanPagePreview
@Composable
fun DeveloperPagePreview() = PreviewTheme { DeveloperScreen {} }

@OScanPagePreview
@Composable
fun AboutPagePreview() = PreviewTheme { AboutScreen {} }

@OScanPagePreview
@Composable
fun VaultSetupPagePreview() = PreviewTheme { VaultSetupScreen(null, {}, {}) }

@OScanPagePreview
@Composable
fun VaultBiometricPagePreview() = PreviewTheme { VaultBiometricSetupScreen(true, null, {}, {}) }

@OScanPagePreview
@Composable
fun VaultUnlockPagePreview() = PreviewTheme { VaultUnlockScreen(VaultSessionState.Locked(), null, true, {}, {}, {}) }

@OScanPagePreview
@Composable
fun VaultLockoutPagePreview() = PreviewTheme { VaultUnlockScreen(VaultSessionState.Locked(24), null, false, {}, {}, {}) }

@OScanPagePreview
@Composable
fun VaultTrashPagePreview() = PreviewTheme {
    VaultTrashScreen(
        trashDocuments = listOf(VaultDocument("vault-preview", "Private contract", true, 0L, 0L, 1L)),
        onRestore = {}, onPermanentlyDelete = {}, onEmptyTrash = {}, onBack = {}
    )
}

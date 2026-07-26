package com.oscan.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DocumentSort { MODIFIED_DESC, MODIFIED_ASC, CREATED_DESC, CREATED_ASC, NAME_ASC, NAME_DESC }
enum class LibraryPresentation { GRID, LIST }
enum class ThemeChoice { SYSTEM, LIGHT, DARK }
enum class AccentTheme {
    TEAL,
    MINT,
    BLUE,
    INDIGO,
    PURPLE,
    PINK,
    CRIMSON,
    AMBER,
    LIME,
    SLATE
}
enum class PdfPageSize { A4, LETTER, MATCH_PAGE }
enum class JpegQuality(
    val qualityInt: Int,
    val pdfMaxDimension: Int,
    val pdfJpegQuality: Int
) {
    HIGH(90, 2_560, 90),
    MEDIUM(80, 1_920, 78),
    LOW(60, 1_280, 60)
}
enum class CameraLensPreference { BACK, FRONT }

data class UserPreferences(
    val documentSort: DocumentSort = DocumentSort.MODIFIED_DESC,
    val libraryPresentation: LibraryPresentation = LibraryPresentation.GRID,
    val themeChoice: ThemeChoice = ThemeChoice.SYSTEM,
    val accentTheme: AccentTheme = AccentTheme.TEAL,
    val displayName: String = "Local Workspace",
    val avatarPreset: String = "TEAL",
    val autoCaptureDefault: Boolean = false,
    val shutterFeedback: Boolean = true,
    val cameraLensPreference: CameraLensPreference = CameraLensPreference.BACK,
    val defaultTreatment: String = "MAGIC",
    val defaultExportFilenamePattern: String = "OScan_{DATE}",
    val defaultPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultJpegQuality: JpegQuality = JpegQuality.HIGH
)

private val Context.oscanPreferences by preferencesDataStore(name = "user_preferences")

class UserPreferencesStore(private val context: Context) {
    val preferences: Flow<UserPreferences> = context.oscanPreferences.data.map { values ->
        UserPreferences(
            documentSort = values.enumValue(Keys.SORT, DocumentSort.MODIFIED_DESC),
            libraryPresentation = values.enumValue(Keys.PRESENTATION, LibraryPresentation.GRID),
            themeChoice = values.enumValue(Keys.THEME, ThemeChoice.SYSTEM),
            accentTheme = values.enumValue(Keys.ACCENT_THEME, AccentTheme.TEAL),
            displayName = values[Keys.DISPLAY_NAME] ?: "Local Workspace",
            avatarPreset = values[Keys.AVATAR_PRESET] ?: "TEAL",
            autoCaptureDefault = values[Keys.AUTO_CAPTURE_DEFAULT] ?: false,
            shutterFeedback = values[Keys.SHUTTER_FEEDBACK] ?: true,
            cameraLensPreference = values.enumValue(Keys.CAMERA_LENS, CameraLensPreference.BACK),
            defaultTreatment = values[Keys.DEFAULT_TREATMENT] ?: "MAGIC",
            defaultExportFilenamePattern = values[Keys.EXPORT_FILENAME_PATTERN] ?: "OScan_{DATE}",
            defaultPageSize = values.enumValue(Keys.DEFAULT_PAGE_SIZE, PdfPageSize.A4),
            defaultJpegQuality = values.enumValue(Keys.DEFAULT_JPEG_QUALITY, JpegQuality.HIGH)
        )
    }

    suspend fun setDocumentSort(value: DocumentSort) = setEnum(Keys.SORT, value)
    suspend fun setLibraryPresentation(value: LibraryPresentation) = setEnum(Keys.PRESENTATION, value)
    suspend fun setThemeChoice(value: ThemeChoice) = setEnum(Keys.THEME, value)
    suspend fun setAccentTheme(value: AccentTheme) = setEnum(Keys.ACCENT_THEME, value)
    suspend fun setDisplayName(value: String) = context.oscanPreferences.edit { it[Keys.DISPLAY_NAME] = value }
    suspend fun setAvatarPreset(value: String) = context.oscanPreferences.edit { it[Keys.AVATAR_PRESET] = value }
    suspend fun setAutoCaptureDefault(value: Boolean) = context.oscanPreferences.edit { it[Keys.AUTO_CAPTURE_DEFAULT] = value }
    suspend fun setShutterFeedback(value: Boolean) = context.oscanPreferences.edit { it[Keys.SHUTTER_FEEDBACK] = value }
    suspend fun setCameraLensPreference(value: CameraLensPreference) = setEnum(Keys.CAMERA_LENS, value)
    suspend fun setDefaultTreatment(value: String) = context.oscanPreferences.edit { it[Keys.DEFAULT_TREATMENT] = value }
    suspend fun setDefaultExportFilenamePattern(value: String) = context.oscanPreferences.edit { it[Keys.EXPORT_FILENAME_PATTERN] = value }
    suspend fun setDefaultPageSize(value: PdfPageSize) = setEnum(Keys.DEFAULT_PAGE_SIZE, value)
    suspend fun setDefaultJpegQuality(value: JpegQuality) = setEnum(Keys.DEFAULT_JPEG_QUALITY, value)

    private suspend fun setEnum(key: Preferences.Key<String>, value: Enum<*>) {
        context.oscanPreferences.edit { it[key] = value.name }
    }

    private inline fun <reified T : Enum<T>> Preferences.enumValue(
        key: Preferences.Key<String>,
        fallback: T
    ): T = this[key]?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private object Keys {
        val SORT = stringPreferencesKey("document_sort")
        val PRESENTATION = stringPreferencesKey("library_presentation")
        val THEME = stringPreferencesKey("theme_choice")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val AVATAR_PRESET = stringPreferencesKey("avatar_preset")
        val AUTO_CAPTURE_DEFAULT = booleanPreferencesKey("auto_capture_default")
        val SHUTTER_FEEDBACK = booleanPreferencesKey("shutter_feedback")
        val CAMERA_LENS = stringPreferencesKey("camera_lens_preference")
        val DEFAULT_TREATMENT = stringPreferencesKey("default_treatment")
        val EXPORT_FILENAME_PATTERN = stringPreferencesKey("export_filename_pattern")
        val DEFAULT_PAGE_SIZE = stringPreferencesKey("default_page_size")
        val DEFAULT_JPEG_QUALITY = stringPreferencesKey("default_jpeg_quality")
    }
}

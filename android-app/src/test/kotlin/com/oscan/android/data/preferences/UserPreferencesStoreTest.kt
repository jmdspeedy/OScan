package com.oscan.android.data.preferences

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserPreferencesStoreTest {

    private lateinit var store: UserPreferencesStore

    @Before
    fun setup() {
        store = UserPreferencesStore(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun defaultPreferencesValuesAreCorrect() = runTest {
        val prefs = store.preferences.first()
        assertEquals("Local Workspace", prefs.displayName)
        assertEquals("TEAL", prefs.avatarPreset)
        assertFalse(prefs.autoCaptureDefault)
        assertTrue(prefs.shutterFeedback)
        assertEquals(CameraLensPreference.BACK, prefs.cameraLensPreference)
        assertEquals("MAGIC", prefs.defaultTreatment)
        assertEquals("OScan_{DATE}", prefs.defaultExportFilenamePattern)
        assertEquals(PdfPageSize.A4, prefs.defaultPageSize)
        assertEquals(JpegQuality.HIGH, prefs.defaultJpegQuality)
        assertEquals(ThemeChoice.SYSTEM, prefs.themeChoice)
        assertEquals(AccentTheme.TEAL, prefs.accentTheme)
    }

    @Test
    fun updatePreferencesPersistsValues() = runTest {
        store.setDisplayName("Custom User")
        store.setAvatarPreset("INDIGO")
        store.setAutoCaptureDefault(true)
        store.setShutterFeedback(false)
        store.setDefaultTreatment("ORIGINAL")
        store.setDefaultPageSize(PdfPageSize.LETTER)
        store.setDefaultJpegQuality(JpegQuality.MEDIUM)
        store.setThemeChoice(ThemeChoice.DARK)
        store.setAccentTheme(AccentTheme.PURPLE)

        val updated = store.preferences.first()
        assertEquals("Custom User", updated.displayName)
        assertEquals("INDIGO", updated.avatarPreset)
        assertTrue(updated.autoCaptureDefault)
        assertFalse(updated.shutterFeedback)
        assertEquals("ORIGINAL", updated.defaultTreatment)
        assertEquals(PdfPageSize.LETTER, updated.defaultPageSize)
        assertEquals(JpegQuality.MEDIUM, updated.defaultJpegQuality)
        assertEquals(ThemeChoice.DARK, updated.themeChoice)
        assertEquals(AccentTheme.PURPLE, updated.accentTheme)
    }
}

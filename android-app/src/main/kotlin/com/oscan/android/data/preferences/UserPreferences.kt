package com.oscan.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DocumentSort { MODIFIED_DESC, MODIFIED_ASC, CREATED_DESC, CREATED_ASC, NAME_ASC, NAME_DESC }
enum class LibraryPresentation { GRID, LIST }
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val documentSort: DocumentSort = DocumentSort.MODIFIED_DESC,
    val libraryPresentation: LibraryPresentation = LibraryPresentation.GRID,
    val themeChoice: ThemeChoice = ThemeChoice.SYSTEM
)

private val Context.oscanPreferences by preferencesDataStore(name = "user_preferences")

class UserPreferencesStore(private val context: Context) {
    val preferences: Flow<UserPreferences> = context.oscanPreferences.data.map { values ->
        UserPreferences(
            documentSort = values.enumValue(Keys.SORT, DocumentSort.MODIFIED_DESC),
            libraryPresentation = values.enumValue(Keys.PRESENTATION, LibraryPresentation.GRID),
            themeChoice = values.enumValue(Keys.THEME, ThemeChoice.SYSTEM)
        )
    }

    suspend fun setDocumentSort(value: DocumentSort) = set(Keys.SORT, value)
    suspend fun setLibraryPresentation(value: LibraryPresentation) = set(Keys.PRESENTATION, value)
    suspend fun setThemeChoice(value: ThemeChoice) = set(Keys.THEME, value)

    private suspend fun set(key: Preferences.Key<String>, value: Enum<*>) {
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
    }
}

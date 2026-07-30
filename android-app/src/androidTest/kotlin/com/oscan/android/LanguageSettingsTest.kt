package com.oscan.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.oscan.android.data.preferences.AppLanguage
import com.oscan.android.ui.LanguageSettingsScreen
import com.oscan.android.ui.theme.OScanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LanguageSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun languageSettingsScreen_displaysAllOptions() {
        composeRule.setContent {
            OScanTheme {
                LanguageSettingsScreen(
                    selectedLanguage = AppLanguage.SYSTEM,
                    onLanguageSelected = {},
                    onBackClick = {}
                )
            }
        }

        composeRule.onNodeWithText("App language").assertIsDisplayed()
        composeRule.onNodeWithText("System default").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Currently:", substring = true).fetchSemanticsNodes().size
        )
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("简体中文").assertIsDisplayed()
        composeRule.onNodeWithText("日本語").assertIsDisplayed()
    }

    @Test
    fun languageSettingsScreen_reflectsSelectionAndTriggersCallback() {
        var selected: AppLanguage = AppLanguage.ENGLISH

        composeRule.setContent {
            OScanTheme {
                LanguageSettingsScreen(
                    selectedLanguage = selected,
                    onLanguageSelected = { selected = it },
                    onBackClick = {}
                )
            }
        }

        composeRule.onNodeWithText("English").assertIsSelected()

        composeRule.onNodeWithText("简体中文").performClick()
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, selected)
    }
}

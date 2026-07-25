package com.oscan.android.localization

import androidx.core.os.LocaleListCompat
import com.oscan.android.data.preferences.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocaleControllerTest {

    @Test
    fun appLanguage_fromLanguageTag_mapsKnownTags() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("   "))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTag("zh-CN"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTag("ZH-cn"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromLanguageTag("ja"))
    }

    @Test
    fun appLanguage_fromLanguageTag_unsupportedReturnsSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("fr"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("de-DE"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("es"))
    }

    @Test
    fun appLanguage_languageTagPropertiesAreCorrect() {
        assertNull(AppLanguage.SYSTEM.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
        assertEquals("zh-CN", AppLanguage.SIMPLIFIED_CHINESE.languageTag)
        assertEquals("ja", AppLanguage.JAPANESE.languageTag)
    }

    @Test
    fun localeListCompat_forLanguageTags_producesExpectedLocales() {
        val systemList = LocaleListCompat.getEmptyLocaleList()
        assertEquals(0, systemList.size())

        val enList = LocaleListCompat.forLanguageTags(AppLanguage.ENGLISH.languageTag)
        assertEquals(1, enList.size())
        assertEquals("en", enList.get(0)?.language)

        val zhList = LocaleListCompat.forLanguageTags(AppLanguage.SIMPLIFIED_CHINESE.languageTag)
        assertEquals(1, zhList.size())
        assertEquals("zh", zhList.get(0)?.language)

        val jaList = LocaleListCompat.forLanguageTags(AppLanguage.JAPANESE.languageTag)
        assertEquals(1, jaList.size())
        assertEquals("ja", jaList.get(0)?.language)
    }
}

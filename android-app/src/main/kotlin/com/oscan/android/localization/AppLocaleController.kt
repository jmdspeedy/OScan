package com.oscan.android.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.oscan.android.data.preferences.AppLanguage

/**
 * Controller for getting and setting the application's locale configuration.
 */
interface AppLocaleController {
    /**
     * Returns the currently configured [AppLanguage].
     */
    fun currentLanguage(): AppLanguage

    /**
     * Sets the active application language.
     *
     * @param language The desired [AppLanguage] selection.
     */
    fun setLanguage(language: AppLanguage)
}

/**
 * Android implementation of [AppLocaleController] backed by [AppCompatDelegate].
 */
class AndroidAppLocaleController : AppLocaleController {
    override fun currentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            return AppLanguage.SYSTEM
        }
        val firstLocale = locales.get(0) ?: return AppLanguage.SYSTEM
        val tag = firstLocale.toLanguageTag()
        return AppLanguage.fromLanguageTag(tag)
    }

    override fun setLanguage(language: AppLanguage) {
        val localeList = if (language.languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.languageTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}

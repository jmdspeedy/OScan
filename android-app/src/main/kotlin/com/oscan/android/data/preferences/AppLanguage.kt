package com.oscan.android.data.preferences

/**
 * Supported app languages for OScan.
 *
 * @property languageTag The IETF BCP 47 language tag (e.g. "en", "zh-CN", "ja"), or null for system default.
 */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    SIMPLIFIED_CHINESE("zh-CN"),
    JAPANESE("ja");

    companion object {
        /**
         * Resolves an [AppLanguage] from a language tag string.
         * Returns [SYSTEM] for null, empty, or unrecognized tags.
         */
        fun fromLanguageTag(tag: String?): AppLanguage {
            if (tag.isNull_or_blank()) return SYSTEM
            return entries.firstOrNull { it.languageTag.equals(tag, ignoreCase = true) } ?: SYSTEM
        }

        private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
    }
}

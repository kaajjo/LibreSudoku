package com.kaajjo.libresudoku.ui.components.locale_emoji

import android.util.Log

object LocaleEmoji {
    /**
     * Get a country flag from language code
     * @return country flag (emoji)
     */
    fun getFlagEmoji(
        languageCode: String
    ): String? {
        val countryCode = countryFromLanguage(languageCode)
        if (countryCode.isNullOrBlank()) return null

        return countryCodeToEmoji(countryCode)
    }

    private fun countryFromLanguage(languageCode: String?): String? {
        if (languageCode == null) return null

        return languageCodeToCountryCode[languageCode.lowercase()]
    }

    private fun countryCodeToEmoji(countryCode: String): String? {
        val uppercaseCode = countryCode.uppercase()

        try {
            val firstChar = uppercaseCode[0] - 'A' + 0x1F1E6
            val secondChar = uppercaseCode[1] - 'A' + 0x1F1E6
            val emoji = String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
            return emoji
        } catch (e: IllegalArgumentException) {
            Log.e("LocaleEmoji", "Cannot find flag for: $countryCode")
            return null
        }
    }
}
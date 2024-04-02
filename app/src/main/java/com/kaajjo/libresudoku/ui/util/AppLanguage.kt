package com.kaajjo.libresudoku.ui.util

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.kaajjo.libresudoku.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

fun getCurrentLocaleString(context: Context): String {
    val langs = getLangs(context)
    langs.forEach {
        Log.d("lang", "${it.key} ${it.value}")
    }
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales == LocaleListCompat.getEmptyLocaleList()) {
        return context.getString(R.string.label_default)
    }
    return getDisplayName(locales.toLanguageTags())
}

fun getCurrentLocaleTag(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales == LocaleListCompat.getEmptyLocaleList()) {
        return ""
    }
    return locales.toLanguageTags()
}

fun getLangs(context: Context): Map<String, String> {
    val langs = mutableListOf<Pair<String, String>>()
    val parser = context.resources.getXml(R.xml.locales_config)
    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
            for (i in 0 until parser.attributeCount) {
                if (parser.getAttributeName(i) == "name") {
                    val langTag = parser.getAttributeValue(i)
                    val displayName = getDisplayName(langTag)
                    if (displayName.isNotEmpty()) {
                        langs.add(Pair(langTag, displayName))
                    }
                }
            }
        }
        eventType = parser.next()
    }

    langs.sortBy { it.second }
    langs.add(0, Pair("", context.getString(R.string.label_default)))

    return langs.toMap()
}

private fun getDisplayName(lang: String?): String {
    if (lang == null) {
        return ""
    }

    val locale = when (lang) {
        "" -> LocaleListCompat.getAdjustedDefault()[0]
        else -> Locale.forLanguageTag(lang)
    }
    return locale!!.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
}


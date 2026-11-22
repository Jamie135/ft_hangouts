package com.pbureera.ft_hangouts.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The language chosen in the app's Language menu.
 *
 * [SYSTEM] (an empty locale list) means "no override": the app follows the phone's
 * language, which is what the subject requires. Picking English or French overrides
 * the phone's language for this app only, until the user picks System default again.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    FRENCH("fr");

    companion object {
        fun current(): AppLanguage {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return SYSTEM
            val language = locales[0]?.language
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }

        /** Saves the choice and recreates the open screens in the new language. */
        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
}

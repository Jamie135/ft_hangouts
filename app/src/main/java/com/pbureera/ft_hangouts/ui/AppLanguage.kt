package com.pbureera.ft_hangouts.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList

/**
 * The language chosen in the app's Language menu.
 *
 * [SYSTEM] (no override) means the app follows the phone's language, which is what the
 * subject requires. Picking English or French overrides the phone's language for this
 * app only, until the user picks System default again.
 *
 * - Android 13+: the choice is handed to the system's [LocaleManager], which saves it,
 *   recreates the screens and also shows it in Settings > Apps > ft_hangouts > Language.
 * - Older versions: the choice is saved in SharedPreferences and every screen applies
 *   it itself in [BaseActivity.attachBaseContext] through [wrap].
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    FRENCH("fr");

    companion object {
        private const val PREFS = "settings"
        private const val KEY_LANGUAGE = "language"

        fun current(context: Context): AppLanguage {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
                if (locales.isEmpty) "" else locales[0].language
            } else {
                savedTag(context)
            }
            return entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }

        /** Saves the choice and shows the open screen again in the new language. */
        fun apply(activity: Activity, language: AppLanguage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(language.tag)
            } else {
                prefs(activity).edit().putString(KEY_LANGUAGE, language.tag).apply()
                activity.recreate()
            }
        }

        /** Before Android 13: a context using the saved language (unchanged if System default). */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val tag = savedTag(base)
            if (tag.isEmpty()) return base
            val config = Configuration(base.resources.configuration)
            config.setLocales(LocaleList.forLanguageTags(tag))
            return base.createConfigurationContext(config)
        }

        private fun savedTag(context: Context) = prefs(context).getString(KEY_LANGUAGE, "") ?: ""

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}

package com.pbureera.ft_hangouts.ui

import android.content.Context
import com.pbureera.ft_hangouts.R

/**
 * The header (action bar) colour chosen by the user, saved in SharedPreferences
 * so it survives restarting the app.
 */
object HeaderColor {

    private const val PREFS = "settings"
    private const val KEY_COLOR = "header_color"

    /** The saved colour, or null if the user never picked one (the theme's colour is used). */
    fun get(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_COLOR)) prefs.getInt(KEY_COLOR, 0) else null
    }

    fun set(context: Context, color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_COLOR, color)
            .apply()
    }

    /** Theme overlays matching [choices], in the same order (see themes.xml). */
    private val OVERLAYS = intArrayOf(
        R.style.HeaderColor_Blue, R.style.HeaderColor_Red, R.style.HeaderColor_Green,
        R.style.HeaderColor_Purple, R.style.HeaderColor_Orange, R.style.HeaderColor_Teal,
        R.style.HeaderColor_Grey,
    )

    /** The theme overlay for the saved colour (blue if none was picked). */
    fun overlay(context: Context): Int {
        val index = choices(context).indexOf(get(context) ?: return OVERLAYS[0])
        return OVERLAYS.getOrElse(index) { OVERLAYS[0] }
    }

    /** All the colours offered in the menu, in the same order as R.array.header_color_names. */
    fun choices(context: Context): IntArray {
        val array = context.resources.obtainTypedArray(R.array.header_colors)
        try {
            return IntArray(array.length()) { array.getColor(it, 0) }
        } finally {
            array.recycle()
        }
    }
}

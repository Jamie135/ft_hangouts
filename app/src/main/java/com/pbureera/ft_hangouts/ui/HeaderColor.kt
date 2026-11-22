package com.pbureera.ft_hangouts.ui

import android.content.Context
import androidx.annotation.ColorInt
import com.pbureera.ft_hangouts.R

/**
 * The header (action bar) colour chosen by the user, saved in SharedPreferences
 * so it survives restarting the app.
 */
object HeaderColor {

    private const val PREFS = "settings"
    private const val KEY_COLOR = "header_color"

    /** The saved colour, or null if the user never picked one (the theme's colour is used). */
    @ColorInt
    fun get(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_COLOR)) prefs.getInt(KEY_COLOR, 0) else null
    }

    fun set(context: Context, @ColorInt color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_COLOR, color)
            .apply()
    }

    /** All the colours offered in the menu, in the same order as R.array.header_color_names. */
    @ColorInt
    fun choices(context: Context): IntArray {
        val array = context.resources.obtainTypedArray(R.array.header_colors)
        try {
            return IntArray(array.length()) { array.getColor(it, 0) }
        } finally {
            array.recycle()
        }
    }
}

package com.pbureera.ft_hangouts.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets

/**
 * Parent of every screen: theme colour, language, layout below the header and the back arrow.
 */
abstract class BaseActivity : Activity() {

    /** Before Android 13, the language picked in the app's menu is applied here (see [AppLanguage]). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    /**
     * The colour chosen in the "Header colour" menu becomes the theme colour of every screen:
     * header, buttons, avatars, sent bubbles, labels... It must be applied before any view
     * is created, so before super.onCreate().
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        theme.applyStyle(HeaderColor.overlay(this), true)
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            keepContentBetweenSystemBars()
        }
    }

    /**
     * From Android 15 apps are drawn edge-to-edge: the layout starts at the very top of
     * the screen, behind the status bar and the action bar, and the keyboard covers the
     * bottom. Pad the content so it sits below the action bar and above the navigation
     * bar / keyboard.
     */
    private fun keepContentBetweenSystemBars() {
        val content = findViewById<View>(android.R.id.content)
        val actionBar = findViewById<View>(
            resources.getIdentifier("action_bar_container", "id", "android")
        )
        var bars = android.graphics.Insets.NONE

        fun applyPadding() {
            // The action bar already sits below the status bar, so its bottom edge is
            // where our content should start.
            val top = maxOf(bars.top, actionBar?.bottom ?: 0)
            content.setPadding(bars.left, top, bars.right, bars.bottom)
        }

        content.setOnApplyWindowInsetsListener { _, insets ->
            bars = insets.getInsets(
                WindowInsets.Type.systemBars() or
                    WindowInsets.Type.displayCutout() or
                    WindowInsets.Type.ime()
            )
            applyPadding()
            insets
        }
        // The action bar's height is only known after layout (and changes on rotation).
        actionBar?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyPadding() }
        content.requestApplyInsets()
    }

    /** Shows the back arrow in the action bar. */
    protected fun enableUpButton() {
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /** Back arrow in the action bar: close this screen. */
    override fun onNavigateUp(): Boolean {
        finish()
        return true
    }
}

package com.pbureera.ft_hangouts.ui

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets

/**
 * Parent of every screen. Shared behaviour (header colour, "last paused" toast)
 * will live here so each activity gets it for free.
 */
abstract class BaseActivity : Activity() {

    /** Before Android 13, the language picked in the app's menu is applied here (see [AppLanguage]). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
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

    /** Every screen re-applies the colour when it comes to the front, so a change shows everywhere. */
    override fun onResume() {
        super.onResume()
        applyHeaderColor()
    }

    /** Paints the action bar with the colour chosen in the "Header colour" menu, if any. */
    protected fun applyHeaderColor() {
        val color = HeaderColor.get(this) ?: return
        actionBar?.setBackgroundDrawable(ColorDrawable(color))
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

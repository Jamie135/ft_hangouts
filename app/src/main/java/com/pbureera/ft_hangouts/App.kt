package com.pbureera.ft_hangouts

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

/**
 * Application class (declared in the manifest with android:name), created once per
 * process before any screen. It watches every activity's lifecycle to know when the
 * whole app goes to the background and comes back, which a single activity cannot
 * tell on its own: switching screens also stops an activity.
 */
class App : Application(), Application.ActivityLifecycleCallbacks {

    /** Number of activities currently started (visible). 0 = app in the background. */
    private var startedActivities = 0
    private var changingConfiguration = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0 && !changingConfiguration) onAppForeground(activity)
        startedActivities++
        changingConfiguration = false
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        changingConfiguration = activity.isChangingConfigurations
        if (startedActivities == 0 && !changingConfiguration) onAppBackground()
    }

    private fun onAppBackground() {
        prefs().edit().putLong(KEY_LAST_BACKGROUND, System.currentTimeMillis()).apply()
    }

    private fun onAppForeground(activity: Activity) {
        val time = prefs().getLong(KEY_LAST_BACKGROUND, 0L)
        if (time == 0L) return // never been in the background yet

        // Format with the screen's locale so the date follows the app's language.
        val locale = activity.resources.configuration.locales[0]
        val formatted = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)
            .format(Date(time))
        Toast.makeText(
            activity,
            activity.getString(R.string.last_background, formatted),
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Callbacks we don't need.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private companion object {
        const val PREFS = "lifecycle"
        const val KEY_LAST_BACKGROUND = "last_background_time"
    }
}

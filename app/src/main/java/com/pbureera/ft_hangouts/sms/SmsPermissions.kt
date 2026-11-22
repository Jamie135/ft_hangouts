package com.pbureera.ft_hangouts.sms

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager

/**
 * Sending and receiving SMS are "dangerous" permissions: declared in the manifest AND
 * granted by the user at runtime. They belong to the same group, so asking for both
 * at once shows a single dialog.
 */
object SmsPermissions {

    val ALL = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)

    fun isGranted(activity: Activity, permission: String) =
        activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun allGranted(activity: Activity) = ALL.all { isGranted(activity, it) }
}

package com.pbureera.ft_hangouts.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives incoming SMS. Registered in the manifest in the SMS step. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: parse SMS, store it, notify UI
    }
}

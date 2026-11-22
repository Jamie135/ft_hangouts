package com.pbureera.ft_hangouts.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.pbureera.ft_hangouts.R

/**
 * Called by Android after each SMS part was handed to the network.
 * resultCode is RESULT_OK on success, or an SmsManager.RESULT_ERROR_* code.
 */
class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, R.string.error_sms_failed, Toast.LENGTH_LONG).show()
        }
    }
}

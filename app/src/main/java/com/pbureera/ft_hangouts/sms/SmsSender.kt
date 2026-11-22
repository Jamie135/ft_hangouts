package com.pbureera.ft_hangouts.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager

/** Sends SMS through the phone's SmsManager. Needs the SEND_SMS permission. */
object SmsSender {

    /**
     * Sends [body] to [phone]. Long texts are split into several SMS parts and joined
     * again by the receiving phone. Throws if the phone cannot send SMS at all
     * (no SIM / no telephony); a failure reported later by the network is shown by
     * [SmsSentReceiver].
     */
    fun send(context: Context, phone: String, body: String) {
        val smsManager: SmsManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

        val parts = smsManager.divideMessage(body)
        // Android broadcasts this intent once per part, with the result of the send.
        val sentIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SmsSentReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val sentIntents = ArrayList(List(parts.size) { sentIntent })
        smsManager.sendMultipartTextMessage(phone, null, parts, sentIntents, null)
    }
}

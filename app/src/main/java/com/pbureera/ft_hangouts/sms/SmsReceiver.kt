package com.pbureera.ft_hangouts.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.ContactDao
import com.pbureera.ft_hangouts.data.Message
import com.pbureera.ft_hangouts.data.MessageDao

/**
 * Declared in the manifest for SMS_RECEIVED, so Android calls it for every incoming SMS,
 * even when the app is closed. Needs the RECEIVE_SMS permission.
 *
 * Messages from saved contacts are stored in our database; others are ignored.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // A long SMS arrives as several parts in one broadcast: join them per sender.
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val bodiesBySender = parts
            .filter { it.originatingAddress != null }
            .groupBy({ it.originatingAddress!! }, { it.messageBody.orEmpty() })
            .mapValues { (_, bodies) -> bodies.joinToString("") }

        val contactDao = ContactDao(context)
        val messageDao = MessageDao(context)
        var stored = false

        for ((sender, body) in bodiesBySender) {
            val contact = contactDao.findByPhone(sender) ?: continue // not a saved contact
            messageDao.insert(
                Message(
                    contactId = contact.id,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    isSent = false,
                )
            )
            stored = true
            Toast.makeText(
                context,
                context.getString(R.string.new_message_from, contact.displayName),
                Toast.LENGTH_SHORT,
            ).show()
        }

        // Tell an open conversation screen to reload. setPackage keeps this broadcast inside our app.
        if (stored) {
            context.sendBroadcast(Intent(ACTION_MESSAGES_CHANGED).setPackage(context.packageName))
        }
    }

    companion object {
        const val ACTION_MESSAGES_CHANGED = "com.pbureera.ft_hangouts.MESSAGES_CHANGED"
    }
}

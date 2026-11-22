package com.pbureera.ft_hangouts.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.ContactDao
import com.pbureera.ft_hangouts.data.Message
import com.pbureera.ft_hangouts.data.MessageDao
import com.pbureera.ft_hangouts.sms.SmsPermissions
import com.pbureera.ft_hangouts.sms.SmsReceiver
import com.pbureera.ft_hangouts.sms.SmsSender

/** SMS conversation with one contact: history on top, text field and Send at the bottom. */
class ConversationActivity : BaseActivity() {

    private lateinit var contactDao: ContactDao
    private lateinit var messageDao: MessageDao
    private lateinit var adapter: MessageAdapter
    private lateinit var list: ListView
    private lateinit var input: EditText
    private var contactId = 0L

    /** Reloads the list when SmsReceiver stored a new incoming message. */
    private val messagesChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = loadMessages()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)
        enableUpButton()

        contactDao = ContactDao(this)
        messageDao = MessageDao(this)
        contactId = intent.getLongExtra(EXTRA_CONTACT_ID, 0L)

        adapter = MessageAdapter(layoutInflater)
        list = findViewById(R.id.message_list)
        list.adapter = adapter
        list.emptyView = findViewById(R.id.empty_messages)
        input = findViewById(R.id.message_input)
        findViewById<ImageButton>(R.id.button_send).setOnClickListener { onSendClicked() }
    }

    /** Reload on every return: the contact may have been renamed, and messages may have arrived. */
    override fun onResume() {
        super.onResume()
        val contact = contactDao.getById(contactId) ?: run { finish(); return }
        title = contact.displayName
        loadMessages()

        // Only listen while visible; RECEIVER_NOT_EXPORTED: other apps cannot send us this.
        val filter = IntentFilter(SmsReceiver.ACTION_MESSAGES_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(messagesChanged, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(messagesChanged, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(messagesChanged)
    }

    private fun loadMessages() {
        adapter.submit(messageDao.getForContact(contactId))
        list.setSelection(adapter.count - 1) // scroll to the newest message
    }

    private fun onSendClicked() {
        if (input.text.isBlank()) return
        // Since Android 6, "dangerous" permissions must also be granted at runtime.
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(SmsPermissions.ALL, REQUEST_SEND_SMS)
            return // the message stays in the text field; we send it once the user answers
        }
        send(input.text.toString().trim())
    }

    @Deprecated("Framework callback; the replacement needs the AndroidX Activity Result API.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_SEND_SMS) return
        if (SmsPermissions.isGranted(this, Manifest.permission.SEND_SMS)) {
            onSendClicked()
        } else {
            Toast.makeText(this, R.string.error_sms_permission, Toast.LENGTH_LONG).show()
        }
    }

    private fun send(body: String) {
        val contact = contactDao.getById(contactId) ?: return
        try {
            SmsSender.send(this, contact.phone, body)
        } catch (e: Exception) {
            // e.g. a device without telephony: nothing was sent, keep the text so the user can retry.
            Toast.makeText(this, R.string.error_sms_failed, Toast.LENGTH_LONG).show()
            return
        }
        messageDao.insert(
            Message(contactId = contactId, body = body, timestamp = System.currentTimeMillis(), isSent = true)
        )
        input.text.clear()
        loadMessages()
    }

    private companion object {
        const val REQUEST_SEND_SMS = 1
    }
}

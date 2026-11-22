package com.pbureera.ft_hangouts.ui

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.ContactDao
import com.pbureera.ft_hangouts.sms.SmsPermissions
import com.pbureera.ft_hangouts.sms.SmsReceiver

/** Home screen: a summary row for every saved contact. */
class MainActivity : BaseActivity() {

    private lateinit var dao: ContactDao
    private lateinit var adapter: ContactAdapter

    /** The settings dialog currently shown (header colour or language), if any. */
    private var dialog: AlertDialog? = null
    private var dialogKind: String? = null

    /** An SMS from an unknown number adds a contact: show it straight away. */
    private val messagesChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = adapter.submit(dao.getAll())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dao = ContactDao(this)
        adapter = ContactAdapter(layoutInflater)

        val list = findViewById<ListView>(R.id.contact_list)
        list.adapter = adapter
        list.emptyView = findViewById(R.id.empty_text)
        list.setOnItemClickListener { _, _, _, id ->
            startActivity(
                Intent(this, ContactDetailActivity::class.java)
                    .putExtra(EXTRA_CONTACT_ID, id)
            )
        }

        // SMS can arrive at any time, so ask for the SMS permissions as soon as the app starts
        // (only on a fresh start, not after every rotation).
        if (savedInstanceState == null && !SmsPermissions.allGranted(this)) {
            requestPermissions(SmsPermissions.ALL, REQUEST_SMS_PERMISSIONS)
        }

        // Dialogs are destroyed on rotation: show the one that was open again.
        when (savedInstanceState?.getString(STATE_OPEN_DIALOG)) {
            DIALOG_COLOR -> showHeaderColorDialog()
            DIALOG_LANGUAGE -> showLanguageDialog()
        }
    }

    /**
     * Runs every time the screen comes back to the front, so the list is up to date
     * after adding, editing or deleting a contact on another screen.
     */
    override fun onResume() {
        super.onResume()
        adapter.submit(dao.getAll())
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_add_contact -> {
            startActivity(Intent(this, ContactEditActivity::class.java))
            true
        }
        R.id.action_header_color -> {
            showHeaderColorDialog()
            true
        }
        R.id.action_language -> {
            showLanguageDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /** A list of colours with the current one ticked; picking one applies and saves it. */
    private fun showHeaderColorDialog() {
        val colors = HeaderColor.choices(this)
        val current = colors.indexOf(HeaderColor.get(this) ?: colors[0])

        showChoiceDialog(
            DIALOG_COLOR, R.string.action_header_color,
            resources.getStringArray(R.array.header_color_names), current,
        ) { which ->
            HeaderColor.set(this, colors[which])
            applyHeaderColor()
        }
    }

    /** System default / English / Français, with the current choice ticked. */
    private fun showLanguageDialog() {
        val languages = AppLanguage.entries
        val names = languages.map {
            getString(
                when (it) {
                    AppLanguage.SYSTEM -> R.string.language_system
                    AppLanguage.ENGLISH -> R.string.language_english
                    AppLanguage.FRENCH -> R.string.language_french
                }
            )
        }.toTypedArray()

        showChoiceDialog(
            DIALOG_LANGUAGE, R.string.action_language,
            names, languages.indexOf(AppLanguage.current(this)),
        ) { which ->
            // Android recreates every open screen in the new language.
            if (languages[which] != AppLanguage.current(this)) AppLanguage.apply(this, languages[which])
        }
    }

    /** A single-choice dialog that closes and calls [onPick] as soon as an item is tapped. */
    private fun showChoiceDialog(
        kind: String,
        titleRes: Int,
        items: Array<String>,
        checked: Int,
        onPick: (Int) -> Unit,
    ) {
        dialogKind = kind
        dialog = AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setSingleChoiceItems(items, checked) { d, which ->
                d.dismiss()
                onPick(which)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setOnDismissListener {
                dialog = null
                dialogKind = null
            }
            .show()
    }

    @Deprecated("Framework callback; the replacement needs the AndroidX Activity Result API.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSIONS &&
            !SmsPermissions.isGranted(this, Manifest.permission.RECEIVE_SMS)
        ) {
            Toast.makeText(this, R.string.error_sms_receive_permission, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (dialog?.isShowing == true) outState.putString(STATE_OPEN_DIALOG, dialogKind)
    }

    override fun onDestroy() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        super.onDestroy()
    }

    private companion object {
        const val REQUEST_SMS_PERMISSIONS = 1
        const val STATE_OPEN_DIALOG = "open_dialog"
        const val DIALOG_COLOR = "color"
        const val DIALOG_LANGUAGE = "language"
    }
}

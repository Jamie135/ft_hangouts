package com.pbureera.ft_hangouts.ui

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Contact
import com.pbureera.ft_hangouts.data.ContactDao
import com.pbureera.ft_hangouts.data.PhotoStore

/** Shows one contact's details, with Edit and Delete in the menu. */
class ContactDetailActivity : BaseActivity() {

    private lateinit var dao: ContactDao
    private lateinit var rows: LinearLayout
    private var contactId = 0L
    private var contact: Contact? = null
    private var deleteDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_detail)
        enableUpButton()

        dao = ContactDao(this)
        rows = findViewById(R.id.detail_rows)
        contactId = intent.getLongExtra(EXTRA_CONTACT_ID, 0L)
        setUpQuickAction(R.id.button_call, R.drawable.ic_call, R.string.action_call) { call() }
        setUpQuickAction(R.id.button_message, R.drawable.ic_message, R.string.action_message) {
            startActivity(
                Intent(this, ConversationActivity::class.java)
                    .putExtra(EXTRA_CONTACT_ID, contactId)
            )
        }

        if (!load()) return
        // Dialogs are destroyed on rotation: show it again if it was open.
        if (savedInstanceState?.getBoolean(STATE_DELETE_DIALOG) == true) confirmDelete()
    }

    /** Coming back from the edit screen: show the updated values. */
    override fun onRestart() {
        super.onRestart()
        load()
    }

    /** Reads the contact from the database and fills the screen. Closes it if the contact is gone. */
    private fun load(): Boolean {
        val c = dao.getById(contactId)
        if (c == null) {
            finish()
            return false
        }
        contact = c
        title = c.displayName
        Avatar.bind(findViewById(R.id.detail_avatar), c)

        // Header: full name, and the company under it (unless the company is the name).
        findViewById<TextView>(R.id.detail_name).text = c.displayName
        val company = findViewById<TextView>(R.id.detail_company)
        company.text = c.company
        company.visibility =
            if (c.company.isBlank() || c.company == c.displayName) View.GONE else View.VISIBLE

        rows.removeAllViews()
        addRow(R.drawable.ic_person, R.string.label_first_name, c.firstName)
        addRow(R.drawable.ic_person, R.string.label_last_name, c.lastName)
        addRow(R.drawable.ic_business, R.string.label_company, c.company)
        addRow(R.drawable.ic_call, R.string.label_phone, c.phone)
        addRow(R.drawable.ic_email, R.string.label_email, c.email)
        addRow(R.drawable.ic_place, R.string.label_address, c.address)
        return true
    }

    /** Adds an "icon / value / label" row to the card, skipping fields the user left empty. */
    private fun addRow(iconRes: Int, labelRes: Int, value: String) {
        if (value.isBlank()) return
        val row = layoutInflater.inflate(R.layout.item_detail_row, rows, false)
        row.findViewById<ImageView>(R.id.row_icon).setImageResource(iconRes)
        row.findViewById<TextView>(R.id.row_label).setText(labelRes)
        row.findViewById<TextView>(R.id.row_value).text = value
        rows.addView(row)
    }

    /** Fills one of the round buttons under the picture. */
    private fun setUpQuickAction(id: Int, iconRes: Int, labelRes: Int, onClick: () -> Unit) {
        val action = findViewById<View>(id)
        val button = action.findViewById<ImageButton>(R.id.quick_action_icon)
        button.setImageResource(iconRes)
        button.contentDescription = getString(labelRes)
        button.setOnClickListener { onClick() }
        action.findViewById<TextView>(R.id.quick_action_label).setText(labelRes)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contact_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit -> {
            startActivity(
                Intent(this, ContactEditActivity::class.java)
                    .putExtra(EXTRA_CONTACT_ID, contactId)
            )
            true
        }
        R.id.action_delete -> {
            confirmDelete()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /** Calls the contact, asking for the CALL_PHONE permission the first time. */
    private fun call() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startCall(Intent.ACTION_CALL)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
        }
    }

    @Deprecated("Framework callback; the replacement needs the AndroidX Activity Result API.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CALL_PHONE) return
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        // Refused: open the phone app with the number typed in instead, which needs no
        // permission; the user just presses the call button there.
        startCall(if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL)
    }

    /** ACTION_CALL calls straight away; ACTION_DIAL only fills in the number in the phone app. */
    private fun startCall(action: String) {
        val phone = contact?.phone ?: return
        try {
            startActivity(Intent(action, Uri.fromParts("tel", phone, null)))
        } catch (e: ActivityNotFoundException) {
            // A tablet without a phone app, for example.
            Toast.makeText(this, R.string.error_no_phone_app, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.error_no_phone_app, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDelete() {
        val name = contact?.displayName ?: return
        deleteDialog = AlertDialog.Builder(this)
            .setTitle(R.string.delete_contact_title)
            .setMessage(getString(R.string.delete_contact_message, name))
            .setPositiveButton(R.string.action_delete) { _, _ -> delete() }
            .setNegativeButton(R.string.action_cancel, null)
            .setOnDismissListener { deleteDialog = null }
            .show()
    }

    private fun delete() {
        dao.delete(contactId)
        contact?.photoFile?.let { PhotoStore.delete(this, it) }
        Toast.makeText(this, R.string.contact_deleted, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_DELETE_DIALOG, deleteDialog?.isShowing == true)
    }

    override fun onDestroy() {
        // Avoid a "leaked window" when the activity is destroyed with the dialog open.
        deleteDialog?.setOnDismissListener(null)
        deleteDialog?.dismiss()
        super.onDestroy()
    }

    private companion object {
        const val STATE_DELETE_DIALOG = "delete_dialog_open"
        const val REQUEST_CALL_PHONE = 1
    }
}

package com.pbureera.ft_hangouts.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Contact
import com.pbureera.ft_hangouts.data.ContactDao

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

        rows.removeAllViews()
        addRow(R.string.label_first_name, c.firstName)
        addRow(R.string.label_last_name, c.lastName)
        addRow(R.string.label_company, c.company)
        addRow(R.string.label_phone, c.phone)
        addRow(R.string.label_email, c.email)
        addRow(R.string.label_address, c.address)
        return true
    }

    /** Adds a "label / value" row, skipping fields the user left empty. */
    private fun addRow(labelRes: Int, value: String) {
        if (value.isBlank()) return
        val row = layoutInflater.inflate(R.layout.item_detail_row, rows, false)
        row.findViewById<TextView>(R.id.row_label).setText(labelRes)
        row.findViewById<TextView>(R.id.row_value).text = value
        rows.addView(row)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contact_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_message -> {
            startActivity(
                Intent(this, ConversationActivity::class.java)
                    .putExtra(EXTRA_CONTACT_ID, contactId)
            )
            true
        }
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
    }
}

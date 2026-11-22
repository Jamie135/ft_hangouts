package com.pbureera.ft_hangouts.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.ContactDao

/** Home screen: a summary row for every saved contact. */
class MainActivity : BaseActivity() {

    private lateinit var dao: ContactDao
    private lateinit var adapter: ContactAdapter
    private var colorDialog: AlertDialog? = null

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

        // Dialogs are destroyed on rotation: show it again if it was open.
        if (savedInstanceState?.getBoolean(STATE_COLOR_DIALOG) == true) showHeaderColorDialog()
    }

    /**
     * Runs every time the screen comes back to the front, so the list is up to date
     * after adding, editing or deleting a contact on another screen.
     */
    override fun onResume() {
        super.onResume()
        adapter.submit(dao.getAll())
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
        else -> super.onOptionsItemSelected(item)
    }

    /** A list of colours with the current one ticked; picking one applies and saves it. */
    private fun showHeaderColorDialog() {
        val colors = HeaderColor.choices(this)
        val current = colors.indexOf(HeaderColor.get(this) ?: colors[0])

        colorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.action_header_color)
            .setSingleChoiceItems(R.array.header_color_names, current) { dialog, which ->
                HeaderColor.set(this, colors[which])
                applyHeaderColor()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setOnDismissListener { colorDialog = null }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_COLOR_DIALOG, colorDialog?.isShowing == true)
    }

    override fun onDestroy() {
        colorDialog?.setOnDismissListener(null)
        colorDialog?.dismiss()
        super.onDestroy()
    }

    private companion object {
        const val STATE_COLOR_DIALOG = "color_dialog_open"
    }
}

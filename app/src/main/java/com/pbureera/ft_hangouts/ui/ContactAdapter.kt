package com.pbureera.ft_hangouts.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Contact

/** Turns the list of contacts into rows for the home screen's ListView. */
class ContactAdapter(private val inflater: LayoutInflater) : BaseAdapter() {

    private var contacts: List<Contact> = emptyList()

    fun submit(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    override fun getCount() = contacts.size
    override fun getItem(position: Int): Contact = contacts[position]
    override fun getItemId(position: Int) = contacts[position].id
    override fun hasStableIds() = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Reuse a row that scrolled off screen instead of inflating a new one each time.
        val row = convertView ?: inflater.inflate(R.layout.item_contact, parent, false).also {
            it.tag = RowViews(
                it.findViewById(R.id.section_header),
                it.findViewById(R.id.contact_avatar),
                it.findViewById(R.id.contact_name),
                it.findViewById(R.id.contact_phone),
            )
        }
        val views = row.tag as RowViews
        val contact = contacts[position]

        // The list is sorted by name: show the letter above the first contact of each letter.
        val section = sectionOf(contact)
        val newSection = position == 0 || sectionOf(contacts[position - 1]) != section
        views.header.text = section
        views.header.visibility = if (newSection) View.VISIBLE else View.GONE

        Avatar.bind(views.avatar, contact)
        views.name.text = contact.displayName
        // An automatically created contact is named after its number: don't show it twice.
        views.phone.text = contact.phone
        views.phone.visibility = if (contact.displayName == contact.phone) View.GONE else View.VISIBLE
        return row
    }

    /** "A" for Alice, "#" for a name starting with a digit or symbol (or a bare phone number). */
    private fun sectionOf(contact: Contact): String {
        val first = contact.displayName.trim().firstOrNull() ?: return "#"
        return if (first.isLetter()) first.uppercase() else "#"
    }

    /** Caches the row's child views so findViewById runs once per row, not per bind. */
    private class RowViews(
        val header: TextView,
        val avatar: View,
        val name: TextView,
        val phone: TextView,
    )
}

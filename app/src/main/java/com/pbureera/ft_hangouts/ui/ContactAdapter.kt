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
                it.findViewById(R.id.contact_avatar),
                it.findViewById(R.id.contact_name),
                it.findViewById(R.id.contact_phone),
            )
        }
        val views = row.tag as RowViews
        val contact = contacts[position]
        Avatar.bind(views.avatar, contact)
        views.name.text = contact.displayName
        views.phone.text = contact.phone
        return row
    }

    /** Caches the row's child views so findViewById runs once per row, not per bind. */
    private class RowViews(val avatar: View, val name: TextView, val phone: TextView)
}

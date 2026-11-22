package com.pbureera.ft_hangouts.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.telephony.PhoneNumberUtils
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_ADDRESS
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_COMPANY
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_EMAIL
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_FIRST_NAME
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_ID
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_LAST_NAME
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_PHONE
import com.pbureera.ft_hangouts.data.DbHelper.Companion.C_PHOTO_URI
import com.pbureera.ft_hangouts.data.DbHelper.Companion.T_CONTACTS

/** CRUD queries on the contacts table. */
class ContactDao(private val db: DbHelper) {

    constructor(context: Context) : this(DbHelper.get(context))

    /** Inserts [contact] (its id is ignored) and returns the new row id, or -1 on failure. */
    fun insert(contact: Contact): Long =
        db.writableDatabase.insert(T_CONTACTS, null, contact.toValues())

    /** Updates the row with [Contact.id]. Returns true if a row was changed. */
    fun update(contact: Contact): Boolean =
        db.writableDatabase.update(
            T_CONTACTS, contact.toValues(), "$C_ID = ?", arrayOf(contact.id.toString())
        ) > 0

    /** Deletes the contact; its messages are removed by ON DELETE CASCADE. */
    fun delete(id: Long): Boolean =
        db.writableDatabase.delete(T_CONTACTS, "$C_ID = ?", arrayOf(id.toString())) > 0

    fun getAll(): List<Contact> =
        db.readableDatabase.query(
            T_CONTACTS, null, null, null, null, null,
            // Sort by the name shown in the list: the first name, else the company, else the number.
            "COALESCE(NULLIF($C_FIRST_NAME, ''), NULLIF($C_COMPANY, ''), $C_PHONE) COLLATE NOCASE, " +
                "$C_LAST_NAME COLLATE NOCASE"
        ).use { c -> buildList { while (c.moveToNext()) add(c.toContact()) } }

    fun getById(id: Long): Contact? =
        db.readableDatabase.query(
            T_CONTACTS, null, "$C_ID = ?", arrayOf(id.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) c.toContact() else null }

    /**
     * Finds the contact whose number matches [phone], treating formats like
     * "+33 6 12 34 56 78" and "0612345678" as the same number.
     */
    fun findByPhone(phone: String): Contact? =
        getAll().firstOrNull { PhoneNumberUtils.compare(it.phone, phone) }

    private fun Contact.toValues() = ContentValues().apply {
        put(C_FIRST_NAME, firstName.trim())
        put(C_LAST_NAME, lastName.trim())
        put(C_PHONE, phone.trim())
        put(C_EMAIL, email.trim())
        put(C_ADDRESS, address.trim())
        put(C_COMPANY, company.trim())
        put(C_PHOTO_URI, photoFile)
    }

    private fun Cursor.toContact() = Contact(
        id = getLong(getColumnIndexOrThrow(C_ID)),
        firstName = getString(getColumnIndexOrThrow(C_FIRST_NAME)),
        lastName = getString(getColumnIndexOrThrow(C_LAST_NAME)),
        phone = getString(getColumnIndexOrThrow(C_PHONE)),
        email = getString(getColumnIndexOrThrow(C_EMAIL)),
        address = getString(getColumnIndexOrThrow(C_ADDRESS)),
        company = getString(getColumnIndexOrThrow(C_COMPANY)),
        photoFile = getColumnIndexOrThrow(C_PHOTO_URI).let { if (isNull(it)) null else getString(it) },
    )
}

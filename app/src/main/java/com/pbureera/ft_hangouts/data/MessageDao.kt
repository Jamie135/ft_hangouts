package com.pbureera.ft_hangouts.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.pbureera.ft_hangouts.data.DbHelper.Companion.M_BODY
import com.pbureera.ft_hangouts.data.DbHelper.Companion.M_CONTACT_ID
import com.pbureera.ft_hangouts.data.DbHelper.Companion.M_ID
import com.pbureera.ft_hangouts.data.DbHelper.Companion.M_IS_SENT
import com.pbureera.ft_hangouts.data.DbHelper.Companion.M_TIMESTAMP
import com.pbureera.ft_hangouts.data.DbHelper.Companion.T_MESSAGES

/** Insert / list queries on the messages table. */
class MessageDao(private val db: DbHelper) {

    constructor(context: Context) : this(DbHelper.get(context))

    /** Inserts [message] (its id is ignored) and returns the new row id, or -1 on failure. */
    fun insert(message: Message): Long =
        db.writableDatabase.insert(T_MESSAGES, null, ContentValues().apply {
            put(M_CONTACT_ID, message.contactId)
            put(M_BODY, message.body)
            put(M_TIMESTAMP, message.timestamp)
            put(M_IS_SENT, if (message.isSent) 1 else 0)
        })

    /** The whole conversation with one contact, oldest first. */
    fun getForContact(contactId: Long): List<Message> =
        db.readableDatabase.query(
            T_MESSAGES, null, "$M_CONTACT_ID = ?", arrayOf(contactId.toString()),
            null, null, "$M_TIMESTAMP ASC, $M_ID ASC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.toMessage()) } }

    private fun Cursor.toMessage() = Message(
        id = getLong(getColumnIndexOrThrow(M_ID)),
        contactId = getLong(getColumnIndexOrThrow(M_CONTACT_ID)),
        body = getString(getColumnIndexOrThrow(M_BODY)),
        timestamp = getLong(getColumnIndexOrThrow(M_TIMESTAMP)),
        isSent = getInt(getColumnIndexOrThrow(M_IS_SENT)) == 1,
    )
}

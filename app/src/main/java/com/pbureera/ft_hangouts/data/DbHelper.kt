package com.pbureera.ft_hangouts.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Creates and upgrades the app's own SQLite database (not the system contacts).
 * Use [DbHelper.get] so activities and the SMS receiver share one instance.
 */
class DbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Needed for ON DELETE CASCADE: deleting a contact deletes its messages.
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T_CONTACTS (
                $C_ID         INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_FIRST_NAME TEXT NOT NULL DEFAULT '',
                $C_LAST_NAME  TEXT NOT NULL DEFAULT '',
                $C_PHONE      TEXT NOT NULL,
                $C_EMAIL      TEXT NOT NULL DEFAULT '',
                $C_ADDRESS    TEXT NOT NULL DEFAULT '',
                $C_COMPANY    TEXT NOT NULL DEFAULT '',
                $C_PHOTO_URI  TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_MESSAGES (
                $M_ID         INTEGER PRIMARY KEY AUTOINCREMENT,
                $M_CONTACT_ID INTEGER NOT NULL
                              REFERENCES $T_CONTACTS($C_ID) ON DELETE CASCADE,
                $M_BODY       TEXT NOT NULL,
                $M_TIMESTAMP  INTEGER NOT NULL,
                $M_IS_SENT    INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_contact ON $T_MESSAGES($M_CONTACT_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No released versions yet: rebuild from scratch.
        db.execSQL("DROP TABLE IF EXISTS $T_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $T_CONTACTS")
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "ft_hangouts.db"
        // 2: "birthday" column replaced by "company".
        const val DB_VERSION = 2

        const val T_CONTACTS = "contacts"
        const val C_ID = "id"
        const val C_FIRST_NAME = "first_name"
        const val C_LAST_NAME = "last_name"
        const val C_PHONE = "phone"
        const val C_EMAIL = "email"
        const val C_ADDRESS = "address"
        const val C_COMPANY = "company"
        const val C_PHOTO_URI = "photo_uri"

        const val T_MESSAGES = "messages"
        const val M_ID = "id"
        const val M_CONTACT_ID = "contact_id"
        const val M_BODY = "body"
        const val M_TIMESTAMP = "timestamp"
        const val M_IS_SENT = "is_sent"

        @Volatile private var instance: DbHelper? = null

        fun get(context: Context): DbHelper =
            instance ?: synchronized(this) {
                instance ?: DbHelper(context).also { instance = it }
            }
    }
}

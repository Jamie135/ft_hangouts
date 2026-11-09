package com.jamie.ft_hangouts.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Creates and upgrades the SQLite database. */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // TODO: CREATE TABLE contacts / messages
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // TODO: migrations
    }

    companion object {
        const val DB_NAME = "ft_hangouts.db"
        const val DB_VERSION = 1
    }
}

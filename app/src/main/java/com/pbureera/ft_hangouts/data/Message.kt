package com.pbureera.ft_hangouts.data

/** One SMS, sent or received. [timestamp] is in milliseconds since the epoch. */
data class Message(
    val id: Long = 0,
    val contactId: Long,
    val body: String,
    val timestamp: Long,
    /** true = we sent it to the contact, false = the contact sent it to us. */
    val isSent: Boolean,
)

package com.pbureera.ft_hangouts.data

/** One row of the contacts table. [id] is 0 until the contact has been inserted. */
data class Contact(
    val id: Long = 0,
    val firstName: String,
    val lastName: String = "",
    val phone: String,
    val email: String = "",
    val address: String = "",
    val birthday: String = "",
    val photoUri: String? = null,
) {
    /** "First Last", or just the first name when there is no last name. */
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

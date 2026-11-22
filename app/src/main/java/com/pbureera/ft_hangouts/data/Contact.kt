package com.pbureera.ft_hangouts.data

/**
 * One row of the contacts table. [id] is 0 until the contact has been inserted.
 * A contact is either a person (first name) or an organisation (company), or both.
 */
data class Contact(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String,
    val email: String = "",
    val address: String = "",
    val company: String = "",
    val photoUri: String? = null,
) {
    /** "First Last" for a person, or the company name for an organisation. */
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifEmpty { company }
}

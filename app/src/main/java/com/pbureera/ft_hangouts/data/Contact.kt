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
    /** File name of the contact's picture in the app's private storage (see PhotoStore), or null. */
    val photoFile: String? = null,
) {
    /**
     * "First Last" for a person, the company name for an organisation, or the phone number
     * for a contact created automatically from an unknown sender (no name yet).
     */
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifEmpty { company }
            .ifEmpty { phone }

    /** "BS" for Bob Smith; the first letter of the company for an organisation; "#" without a name. */
    val initials: String
        get() = initialsOf(firstName, lastName, company)

    companion object {
        fun initialsOf(firstName: String, lastName: String, company: String): String =
            listOf(firstName, lastName)
                .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { company.trim().take(1).uppercase() }
                .ifEmpty { "#" }
    }
}

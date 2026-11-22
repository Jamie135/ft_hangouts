package com.pbureera.ft_hangouts.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Contact
import com.pbureera.ft_hangouts.data.ContactDao

/**
 * Create / edit a contact.
 * Without [EXTRA_CONTACT_ID] it creates a new contact; with it, it edits that one.
 */
class ContactEditActivity : BaseActivity() {

    private lateinit var dao: ContactDao
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var phone: EditText
    private lateinit var email: EditText
    private lateinit var address: EditText
    private lateinit var company: EditText

    /** The contact being edited, or null when creating a new one. */
    private var original: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_edit)
        enableUpButton()

        dao = ContactDao(this)
        firstName = findViewById(R.id.edit_first_name)
        lastName = findViewById(R.id.edit_last_name)
        phone = findViewById(R.id.edit_phone)
        email = findViewById(R.id.edit_email)
        address = findViewById(R.id.edit_address)
        company = findViewById(R.id.edit_company)

        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, 0L)
        if (contactId != 0L) {
            original = dao.getById(contactId) ?: run { finish(); return }
            setTitle(R.string.title_edit_contact)
            // After a rotation the EditTexts restore what the user typed by themselves,
            // so only pre-fill on the first launch.
            if (savedInstanceState == null) fillFields(original!!)
        } else {
            setTitle(R.string.title_new_contact)
        }

        findViewById<Button>(R.id.button_save).setOnClickListener { save() }
    }

    private fun fillFields(c: Contact) {
        firstName.setText(c.firstName)
        lastName.setText(c.lastName)
        phone.setText(c.phone)
        email.setText(c.email)
        address.setText(c.address)
        company.setText(c.company)
    }

    private fun save() {
        if (!validate()) return

        val contact = Contact(
            id = original?.id ?: 0L,
            firstName = firstName.text.toString().trim(),
            lastName = lastName.text.toString().trim(),
            phone = phone.text.toString().trim(),
            email = email.text.toString().trim(),
            address = address.text.toString().trim(),
            company = company.text.toString().trim(),
            photoUri = original?.photoUri,
        )

        if (original != null) {
            if (!dao.update(contact)) return showSaveError()
        } else {
            val newId = dao.insert(contact)
            if (newId == -1L) return showSaveError()
            // Show the new contact straight away.
            startActivity(
                Intent(this, ContactDetailActivity::class.java)
                    .putExtra(EXTRA_CONTACT_ID, newId)
            )
        }
        Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    /** Marks every invalid field and focuses the first one. Returns true if all are valid. */
    private fun validate(): Boolean {
        val errors = mutableListOf<Pair<EditText, Int>>()

        // A person needs a first name, an organisation a company name: at least one of them.
        if (firstName.text.isBlank() && company.text.isBlank()) {
            errors += firstName to R.string.error_name_or_company
            errors += company to R.string.error_name_or_company
        }

        val phoneText = phone.text.toString().trim()
        when {
            phoneText.isEmpty() -> errors += phone to R.string.error_required
            !Patterns.PHONE.matcher(phoneText).matches() -> errors += phone to R.string.error_invalid_phone
        }

        val emailText = email.text.toString().trim()
        if (emailText.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            errors += email to R.string.error_invalid_email
        }

        errors.forEach { (field, message) -> field.error = getString(message) }
        errors.firstOrNull()?.first?.requestFocus()
        return errors.isEmpty()
    }

    private fun showSaveError() {
        Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_LONG).show()
    }
}

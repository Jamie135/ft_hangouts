package com.pbureera.ft_hangouts.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Contact
import com.pbureera.ft_hangouts.data.ContactDao
import com.pbureera.ft_hangouts.data.PhotoStore

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
    private lateinit var avatar: View
    private lateinit var photoButton: Button
    private lateinit var removePhotoButton: Button

    /** The contact being edited, or null when creating a new one. */
    private var original: Contact? = null

    /** The picture currently shown in the form (a PhotoStore file name), or null. */
    private var photo: String? = null

    /**
     * Pictures copied into the app while on this screen. The ones not kept when saving
     * (or all of them when leaving without saving) are deleted, so no unused file stays behind.
     */
    private var importedPhotos = ArrayList<String>()
    private var saved = false

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
        avatar = findViewById(R.id.edit_avatar)
        photoButton = findViewById(R.id.button_photo)
        removePhotoButton = findViewById(R.id.button_remove_photo)

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

        if (savedInstanceState == null) {
            photo = original?.photoFile
        } else {
            photo = savedInstanceState.getString(STATE_PHOTO)
            importedPhotos = savedInstanceState.getStringArrayList(STATE_IMPORTED) ?: ArrayList()
        }

        avatar.setOnClickListener { pickPhoto() }
        photoButton.setOnClickListener { pickPhoto() }
        removePhotoButton.setOnClickListener {
            photo = null
            showPhoto()
        }
        // Without a picture the avatar shows the initials: keep them in sync with the typing.
        val refreshInitials = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = showPhoto()
        }
        listOf(firstName, lastName, company).forEach { it.addTextChangedListener(refreshInitials) }
        showPhoto()

        findViewById<Button>(R.id.button_save).setOnClickListener { save() }
    }

    private fun showPhoto() {
        val initials = Contact.initialsOf(
            firstName.text.toString(), lastName.text.toString(), company.text.toString()
        )
        Avatar.bind(avatar, initials, photo)
        photoButton.setText(if (photo == null) R.string.action_add_photo else R.string.action_change_photo)
        removePhotoButton.visibility = if (photo == null) View.GONE else View.VISIBLE
    }

    /**
     * Opens the system's image picker (no storage permission needed): the photo picker on
     * Android 13+ (closes as soon as a photo is tapped), the file chooser before that.
     */
    private fun pickPhoto() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(intent, REQUEST_PHOTO)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_photo, Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Framework callback; the replacement needs the AndroidX Activity Result API.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PHOTO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val name = PhotoStore.import(this, uri)
        if (name == null) {
            Toast.makeText(this, R.string.error_photo, Toast.LENGTH_LONG).show()
            return
        }
        importedPhotos += name
        photo = name
        showPhoto()
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
            photoFile = photo,
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
        saved = true
        // The old picture (if replaced or removed) and any other picked one are no longer used.
        (importedPhotos + listOfNotNull(original?.photoFile))
            .filter { it != photo }
            .forEach { PhotoStore.delete(this, it) }
        Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PHOTO, photo)
        outState.putStringArrayList(STATE_IMPORTED, importedPhotos)
    }

    /** Leaving without saving (back, back arrow): forget the pictures picked here. */
    override fun onDestroy() {
        if (isFinishing && !saved) importedPhotos.forEach { PhotoStore.delete(this, it) }
        super.onDestroy()
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

    private companion object {
        const val REQUEST_PHOTO = 1
        const val STATE_PHOTO = "photo"
        const val STATE_IMPORTED = "imported_photos"
    }
}

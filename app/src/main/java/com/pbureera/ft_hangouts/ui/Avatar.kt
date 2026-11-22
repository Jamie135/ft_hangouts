package com.pbureera.ft_hangouts.ui

import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.pbureera.ft_hangouts.R
import com.pbureera.ft_hangouts.data.Contact
import com.pbureera.ft_hangouts.data.PhotoStore

/**
 * Fills a view_avatar layout: the contact's picture in a circle, or their initials
 * when they have no picture (or it can't be loaded).
 */
object Avatar {

    fun bind(avatar: View, contact: Contact) = bind(avatar, contact.initials, contact.photoFile)

    fun bind(avatar: View, initials: String, photoFile: String?) {
        val initialsView = avatar.findViewById<TextView>(R.id.avatar_initials)
        val photoView = avatar.findViewById<ImageView>(R.id.avatar_photo)

        val photo = photoFile?.let { PhotoStore.load(avatar.context, it) }
        photoView.setImageBitmap(photo)
        photoView.clipToOutline = true // clip the picture to the round background
        photoView.visibility = if (photo != null) View.VISIBLE else View.GONE

        initialsView.text = initials
        // Letters at 40 % of the circle, whatever size the avatar is.
        initialsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, avatar.layoutParams.height * 0.4f)
        initialsView.visibility = if (photo != null) View.INVISIBLE else View.VISIBLE
    }
}

package com.pbureera.ft_hangouts.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Contact pictures, kept as small square JPEGs in the app's private storage
 * (files/photos/). The database only stores the file name.
 *
 * The picked image is copied rather than linked to: a link to the gallery stops
 * working when the original is deleted or the app's read access to it expires.
 */
object PhotoStore {

    /** Width and height of a saved picture, in pixels: sharp enough for the detail screen. */
    private const val SIZE = 512
    private const val DIR = "photos"

    /** Recently shown pictures, so scrolling the list doesn't decode the same file again. */
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /**
     * Copies the image at [uri] (from the picker) into the app as a centred square.
     * Returns the new file name, or null if the image could not be read.
     */
    fun import(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        fun <T> read(block: (InputStream) -> T): T? = resolver.openInputStream(uri)?.use(block)

        return try {
            // 1. Read only the size, to decode a smaller version of big photos (saves memory).
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            read { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2

            // 2. Decode, turn it upright (camera photos store their rotation in EXIF) and crop.
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = read { BitmapFactory.decodeStream(it, null, options) } ?: return null
            val rotation = read { exifRotation(it) } ?: 0
            val square = centerSquare(bitmap, rotation)

            // 3. Save it under a new unique name.
            val name = "${UUID.randomUUID()}.jpg"
            FileOutputStream(File(dir(context), name)).use {
                square.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            name
        } catch (e: Exception) {
            null // unreadable file, permission revoked, not an image...
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** The picture saved as [name], or null if the file is missing. */
    fun load(context: Context, name: String): Bitmap? =
        cache.get(name) ?: BitmapFactory.decodeFile(File(dir(context), name).path)
            ?.also { cache.put(name, it) }

    fun delete(context: Context, name: String) {
        cache.remove(name)
        File(dir(context), name).delete()
    }

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    private fun exifRotation(stream: InputStream): Int =
        when (ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /** The largest centred square of [src], rotated by [rotation] and scaled down to [SIZE]. */
    private fun centerSquare(src: Bitmap, rotation: Int): Bitmap {
        val side = minOf(src.width, src.height)
        val scale = minOf(1f, SIZE.toFloat() / side)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(
            src, (src.width - side) / 2, (src.height - side) / 2, side, side, matrix, true
        )
    }
}

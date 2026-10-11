package com.example.tend

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Profile photo: cached on the phone, stored on the server. */
object Photo {
    private fun file(context: Context) = File(context.filesDir, "avatar.jpg")

    fun cached(context: Context): Bitmap? {
        val f = file(context)
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    fun save(context: Context, bytes: ByteArray) {
        file(context).writeBytes(bytes)
    }

    fun delete(context: Context) {
        file(context).delete()
    }

    /** Downloads the photo from the API and caches it. */
    suspend fun fetch(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.getPhoto()
            val bytes = if (response.isSuccessful) response.body()?.bytes() else null
            if (bytes != null) {
                save(context, bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /** Shrinks a picked image to max 512px and returns it as JPEG bytes. */
    fun prepare(resolver: ContentResolver, uri: Uri, maxSide: Int = 512): ByteArray? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}

object Avatar {
    /** Shows the user's photo if there is one, otherwise their first initial. */
    fun bind(activity: AppCompatActivity, initial: TextView, image: ImageView) {
        val name = Session.displayName.ifBlank { Session.email }
        initial.text = name.trim().firstOrNull()?.uppercase() ?: "?"

        val cached = Photo.cached(activity)
        show(initial, image, cached)

        if (cached == null && Session.hasPhoto) {
            activity.lifecycleScope.launch {
                show(initial, image, Photo.fetch(activity))
            }
        }
    }

    fun show(initial: TextView, image: ImageView, bitmap: Bitmap?) {
        if (bitmap != null) {
            image.setImageBitmap(bitmap)
            image.visibility = View.VISIBLE
            initial.visibility = View.GONE
        } else {
            image.visibility = View.GONE
            initial.visibility = View.VISIBLE
        }
    }
}

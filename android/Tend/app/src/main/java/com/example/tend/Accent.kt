package com.example.tend

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/** The accent colour the user picks in Settings (terracotta, green or blue). */
object Accent {
    fun colorFor(context: Context, name: String): Int {
        val res = when (name) {
            "green" -> R.color.green
            "blue" -> R.color.blue
            else -> R.color.terracotta
        }
        return ContextCompat.getColor(context, res)
    }

    fun color(context: Context): Int = colorFor(context, Session.colour)

    fun tint(button: MaterialButton) {
        button.backgroundTintList = ColorStateList.valueOf(color(button.context))
    }
}

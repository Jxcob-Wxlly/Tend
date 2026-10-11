package com.example.tend

import android.content.Context
import android.content.SharedPreferences

/** Keeps the login token and the user's settings on the phone. */
object Session {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("tend_session", Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(value) = prefs.edit().putString("email", value).apply()

    var displayName: String
        get() = prefs.getString("displayName", "") ?: ""
        set(value) = prefs.edit().putString("displayName", value).apply()

    var weekStartsOn: String
        get() = prefs.getString("weekStartsOn", "Monday") ?: "Monday"
        set(value) = prefs.edit().putString("weekStartsOn", value).apply()

    var colour: String
        get() = prefs.getString("colour", "terracotta") ?: "terracotta"
        set(value) = prefs.edit().putString("colour", value).apply()

    var dailyReminder: Boolean
        get() = prefs.getBoolean("dailyReminder", false)
        set(value) = prefs.edit().putBoolean("dailyReminder", value).apply()

    var hasPhoto: Boolean
        get() = prefs.getBoolean("hasPhoto", false)
        set(value) = prefs.edit().putBoolean("hasPhoto", value).apply()

    val isLoggedIn: Boolean
        get() = !token.isNullOrEmpty()

    fun saveLogin(auth: AuthResponse) {
        token = auth.token
        saveProfile(auth.profile)
    }

    fun saveProfile(p: Profile) {
        email = p.email
        displayName = p.displayName
        weekStartsOn = p.weekStartsOn
        colour = p.colour
        dailyReminder = p.dailyReminder
        hasPhoto = p.hasPhoto
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

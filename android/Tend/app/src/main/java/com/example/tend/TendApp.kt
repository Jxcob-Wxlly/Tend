package com.example.tend

import android.app.Application

class TendApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
        Reminders.createChannel(this)
        if (Session.isLoggedIn && Session.dailyReminder) Reminders.schedule(this, replace = false)
    }
}

package com.example.tend

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.tend.databinding.ActivitySettingsBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class SettingsActivity : BaseActivity() {

    private lateinit var b: ActivitySettingsBinding

    // Current choices (start from what's saved on the phone)
    private var week = Session.weekStartsOn
    private var colour = Session.colour
    private var reminder = Session.dailyReminder
    private var updatingSwitch = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) uploadPhoto(uri)
    }

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setReminder(true)
        } else {
            setSwitchChecked(false)
            toast("Allow notifications for Tend in your phone's settings to get reminders.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Session.isLoggedIn) {
            signOut()
            return
        }

        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        applyInsets(b.root)

        // Top bar
        b.topBar.logoGroup.setOnClickListener { finish() }
        b.topBar.logoutButton.setOnClickListener { signOut() }

        // Profile
        b.nameInput.setText(Session.displayName)
        b.signedInText.text = "Signed in as ${Session.email}"
        b.changePhotoButton.setOnClickListener { pickImage.launch("image/*") }
        b.settingsAvatar.setOnClickListener { pickImage.launch("image/*") }
        b.saveNameButton.setOnClickListener { saveName() }

        // Preferences
        b.weekMonday.setOnClickListener { setWeek("Monday") }
        b.weekSunday.setOnClickListener { setWeek("Sunday") }
        b.swatchTerracotta.setOnClickListener { setColour("terracotta") }
        b.swatchGreen.setOnClickListener { setColour("green") }
        b.swatchBlue.setOnClickListener { setColour("blue") }
        b.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingSwitch) onReminderToggled(isChecked)
        }

        // Password
        b.updatePasswordButton.setOnClickListener { updatePassword() }

        refreshUi()
        loadProfileFromServer()
    }

    // ---------- Showing the current state ----------

    private fun refreshUi() {
        val accent = Accent.colorFor(this, colour)

        // Accent-coloured bits
        b.topBar.logoIcon.imageTintList = ColorStateList.valueOf(accent)
        b.saveNameButton.backgroundTintList = ColorStateList.valueOf(accent)
        b.updatePasswordButton.backgroundTintList = ColorStateList.valueOf(accent)

        // Week starts on
        styleToggleButton(b.weekMonday, week == "Monday", accent)
        styleToggleButton(b.weekSunday, week == "Sunday", accent)

        // Colour swatches
        b.swatchTerracotta.background = swatch(Accent.colorFor(this, "terracotta"), colour == "terracotta")
        b.swatchGreen.background = swatch(Accent.colorFor(this, "green"), colour == "green")
        b.swatchBlue.background = swatch(Accent.colorFor(this, "blue"), colour == "blue")

        // Daily reminder switch
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        b.reminderSwitch.trackTintList = ColorStateList(
            states, intArrayOf(accent, ContextCompat.getColor(this, R.color.switch_off))
        )
        b.reminderSwitch.thumbTintList = ColorStateList(
            states, intArrayOf(Color.WHITE, 0xFFFFFBF2.toInt())
        )
        setSwitchChecked(reminder)

        // Avatars
        Avatar.bind(this, b.topBar.avatarInitial, b.topBar.avatarImage)
        Avatar.bind(this, b.settingsAvatarInitial, b.settingsAvatarImage)
    }

    private fun styleToggleButton(button: MaterialButton, selected: Boolean, accent: Int) {
        if (selected) {
            button.backgroundTintList = ColorStateList.valueOf(accent)
            button.setTextColor(Color.WHITE)
            button.strokeWidth = 0
        } else {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cream_bg))
            button.setTextColor(ContextCompat.getColor(this, R.color.ink))
            button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.input_border))
            button.strokeWidth = dp(1)
        }
    }

    private fun swatch(color: Int, selected: Boolean): Drawable {
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        val layers = mutableListOf<Drawable>()
        if (selected) {
            layers.add(GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), ContextCompat.getColor(this@SettingsActivity, R.color.ink))
            })
        }
        layers.add(dot)
        val drawable = LayerDrawable(layers.toTypedArray())
        drawable.setLayerInset(layers.lastIndex, dp(5), dp(5), dp(5), dp(5))
        return drawable
    }

    private fun setSwitchChecked(checked: Boolean) {
        updatingSwitch = true
        b.reminderSwitch.isChecked = checked
        updatingSwitch = false
    }

    /** Get the latest profile from the server, in case it was changed on another device. */
    private fun loadProfileFromServer() {
        lifecycleScope.launch {
            when (val result = safeCall { ApiClient.api.getProfile() }) {
                is ApiResult.Success -> {
                    Session.saveProfile(result.data)
                    week = Session.weekStartsOn
                    colour = Session.colour
                    reminder = Session.dailyReminder
                    b.nameInput.setText(Session.displayName)
                    b.signedInText.text = "Signed in as ${Session.email}"
                    refreshUi()
                }
                is ApiResult.Error -> handleError(result)
            }
        }
    }

    // ---------- Profile ----------

    private fun saveName() {
        val name = b.nameInput.text.toString().trim()
        if (name.isEmpty()) {
            toast("Your display name can't be empty.")
            return
        }
        lifecycleScope.launch {
            when (val result = safeCall { ApiClient.api.updateProfile(UpdateProfileRequest(name)) }) {
                is ApiResult.Success -> {
                    Session.saveProfile(result.data)
                    Avatar.bind(this@SettingsActivity, b.topBar.avatarInitial, b.topBar.avatarImage)
                    Avatar.bind(this@SettingsActivity, b.settingsAvatarInitial, b.settingsAvatarImage)
                    toast("Name saved")
                }
                is ApiResult.Error -> handleError(result)
            }
        }
    }

    private fun uploadPhoto(uri: Uri) {
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { Photo.prepare(contentResolver, uri) }
            if (bytes == null) {
                toast("Couldn't read that picture.")
                return@launch
            }
            val part = MultipartBody.Part.createFormData(
                "file", "avatar.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
            )
            when (val result = safeCall { ApiClient.api.uploadPhoto(part) }) {
                is ApiResult.Success -> {
                    Photo.save(this@SettingsActivity, bytes)
                    Session.saveProfile(result.data)
                    Avatar.bind(this@SettingsActivity, b.topBar.avatarInitial, b.topBar.avatarImage)
                    Avatar.bind(this@SettingsActivity, b.settingsAvatarInitial, b.settingsAvatarImage)
                    toast("Photo updated")
                }
                is ApiResult.Error -> handleError(result)
            }
        }
    }

    // ---------- Preferences ----------

    private fun setWeek(value: String) {
        if (week == value) return
        week = value
        refreshUi()
        pushSettings()
    }

    private fun setColour(value: String) {
        if (colour == value) return
        colour = value
        refreshUi()
        pushSettings()
    }

    private fun onReminderToggled(isChecked: Boolean) {
        if (isChecked &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            setReminder(isChecked)
        }
    }

    private fun setReminder(on: Boolean) {
        reminder = on
        setSwitchChecked(on)
        if (on) Reminders.schedule(this, replace = true) else Reminders.cancel(this)
        pushSettings()
    }

    /** Saves the choices on the phone straight away, then sends them to the server. */
    private fun pushSettings() {
        Session.weekStartsOn = week
        Session.colour = colour
        Session.dailyReminder = reminder

        lifecycleScope.launch {
            val result = safeCall {
                ApiClient.api.updateSettings(UpdateSettingsRequest(week, colour, reminder))
            }
            if (result is ApiResult.Error) handleError(result)
        }
    }

    // ---------- Password ----------

    private fun updatePassword() {
        val current = b.currentPasswordInput.text.toString()
        val next = b.newPasswordInput.text.toString()

        if (current.isEmpty()) {
            toast("Enter your current password.")
            return
        }
        if (next.length < 6) {
            toast("New password must be at least 6 characters.")
            return
        }

        b.updatePasswordButton.isEnabled = false
        lifecycleScope.launch {
            when (val result = safeCall {
                ApiClient.api.changePassword(ChangePasswordRequest(current, next))
            }) {
                is ApiResult.Success -> {
                    b.currentPasswordInput.text.clear()
                    b.newPasswordInput.text.clear()
                    toast("Password updated")
                }
                is ApiResult.Error -> handleError(result)
            }
            b.updatePasswordButton.isEnabled = true
        }
    }
}

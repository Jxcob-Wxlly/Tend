package com.example.tend

// ---------- Requests ----------
data class RegisterRequest(val email: String, val password: String, val displayName: String)
data class LoginRequest(val email: String, val password: String)
data class UpdateProfileRequest(val displayName: String)
data class UpdateSettingsRequest(val weekStartsOn: String, val colour: String, val dailyReminder: Boolean)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
data class CreateHabitRequest(val name: String, val emoji: String)
data class ToggleRequest(val date: String)

// ---------- Responses ----------
data class Profile(
    val email: String,
    val displayName: String,
    val hasPhoto: Boolean,
    val weekStartsOn: String,
    val colour: String,
    val dailyReminder: Boolean
)

data class AuthResponse(val token: String, val profile: Profile)

data class Habit(
    val id: Int,
    val name: String,
    val emoji: String,
    val streak: Int,
    val doneDates: List<String>
)

data class ToggleResult(val done: Boolean)

package com.example.tend

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.example.tend.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Already signed in? Skip straight to the habits screen.
        if (Session.isLoggedIn) {
            openMain()
            return
        }

        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        applyInsets(b.root)

        val accent = Accent.color(this)
        b.logoIcon.imageTintList = ColorStateList.valueOf(accent)
        b.registerLink.setTextColor(accent)
        Accent.tint(b.loginButton)

        b.loginButton.setOnClickListener { login() }
        b.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else false
        }
        b.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val email = b.emailInput.text.toString().trim()
        val password = b.passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.")
            return
        }

        b.errorText.visibility = android.view.View.GONE
        b.loginButton.isEnabled = false
        b.loginButton.text = "Signing in…"

        lifecycleScope.launch {
            when (val result = safeCall { ApiClient.api.login(LoginRequest(email, password)) }) {
                is ApiResult.Success -> {
                    Photo.delete(this@LoginActivity)
                    Session.saveLogin(result.data)
                    if (Session.dailyReminder) Reminders.schedule(this@LoginActivity, replace = true)
                    openMain()
                }
                is ApiResult.Error -> {
                    showError(result.message)
                    b.loginButton.isEnabled = true
                    b.loginButton.text = "Sign in"
                }
            }
        }
    }

    private fun showError(message: String) {
        b.errorText.text = message
        b.errorText.visibility = android.view.View.VISIBLE
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

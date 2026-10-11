package com.example.tend

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.example.tend.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {

    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        applyInsets(b.root)

        val accent = Accent.color(this)
        b.logoIcon.imageTintList = ColorStateList.valueOf(accent)
        b.loginLink.setTextColor(accent)
        Accent.tint(b.registerButton)

        b.registerButton.setOnClickListener { register() }
        b.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                register()
                true
            } else false
        }
        b.loginLink.setOnClickListener { finish() }
    }

    private fun register() {
        val name = b.nameInput.text.toString().trim()
        val email = b.emailInput.text.toString().trim()
        val password = b.passwordInput.text.toString()

        when {
            name.isEmpty() -> return showError("Please enter a display name.")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                return showError("Please enter a valid email address.")
            password.length < 6 -> return showError("Password must be at least 6 characters.")
        }

        b.errorText.visibility = View.GONE
        b.registerButton.isEnabled = false
        b.registerButton.text = "Creating account…"

        lifecycleScope.launch {
            when (val result = safeCall {
                ApiClient.api.register(RegisterRequest(email, password, name))
            }) {
                is ApiResult.Success -> {
                    Photo.delete(this@RegisterActivity)
                    Session.saveLogin(result.data)
                    startActivity(
                        Intent(this@RegisterActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                is ApiResult.Error -> {
                    showError(result.message)
                    b.registerButton.isEnabled = true
                    b.registerButton.text = "Create account"
                }
            }
        }
    }

    private fun showError(message: String) {
        b.errorText.text = message
        b.errorText.visibility = View.VISIBLE
    }
}

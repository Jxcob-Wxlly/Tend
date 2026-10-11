package com.example.tend

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    /** Keeps content clear of the status bar, navigation bar and keyboard. */
    protected fun applyInsets(root: View) {
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
    }

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Shows an API error, or signs the user out if their login has expired. */
    protected fun handleError(error: ApiResult.Error) {
        if (error.code == 401 && Session.isLoggedIn) {
            signOut()
        } else {
            toast(error.message)
        }
    }

    protected fun signOut() {
        Session.clear()
        Photo.delete(this)
        Reminders.cancel(this)
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}

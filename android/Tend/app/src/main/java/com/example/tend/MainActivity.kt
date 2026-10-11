package com.example.tend

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.GridLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tend.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: HabitAdapter
    private var selectedEmoji = "🌱"

    private val emojiChoices = listOf(
        "🌱", "💧", "📚", "🏃", "🧘", "💪", "🥗", "😴",
        "✍️", "🎸", "🧹", "💊", "☀️", "🎯", "🙏", "🚶"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Session.isLoggedIn) {
            signOut()
            return
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        applyInsets(b.root)

        adapter = HabitAdapter(
            onToggle = { habit, day -> toggle(habit, day) },
            onDelete = { habit -> confirmDelete(habit) }
        )
        b.habitList.layoutManager = LinearLayoutManager(this)
        b.habitList.adapter = adapter

        b.topBar.settingsButton.setOnClickListener { openSettings() }
        b.topBar.avatar.setOnClickListener { openSettings() }
        b.topBar.logoutButton.setOnClickListener { signOut() }

        b.emojiButton.setOnClickListener { pickEmoji() }
        b.addButton.setOnClickListener { addHabit() }
        b.habitInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addHabit()
                true
            } else false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::b.isInitialized) return

        // Re-read settings every time we come back (they may have changed in Settings)
        val accent = Accent.color(this)
        b.topBar.logoIcon.imageTintList = ColorStateList.valueOf(accent)
        Accent.tint(b.addButton)

        b.dateText.text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH))
        b.helloText.text = "Hello, ${Session.displayName}."
        Avatar.bind(this, b.topBar.avatarInitial, b.topBar.avatarImage)

        loadHabits()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun weekDays(today: LocalDate): List<LocalDate> {
        val firstDay = if (Session.weekStartsOn == "Sunday") DayOfWeek.SUNDAY else DayOfWeek.MONDAY
        val start = today.with(TemporalAdjusters.previousOrSame(firstDay))
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    private fun loadHabits() {
        val today = LocalDate.now()
        val week = weekDays(today)

        lifecycleScope.launch {
            val result = safeCall {
                ApiClient.api.getHabits(week.first().toString(), week.last().toString(), today.toString())
            }
            when (result) {
                is ApiResult.Success -> {
                    val habits = result.data
                    adapter.submit(habits, week, today)

                    val doneToday = habits.count { it.doneDates.contains(today.toString()) }
                    b.subtitleText.text = if (habits.isEmpty()) {
                        "Add your first habit below."
                    } else {
                        "$doneToday of ${habits.size} done today."
                    }
                }
                is ApiResult.Error -> handleError(result)
            }
        }
    }

    private fun addHabit() {
        val name = b.habitInput.text.toString().trim()
        if (name.isEmpty()) {
            toast("Give your habit a name first.")
            return
        }

        b.addButton.isEnabled = false
        lifecycleScope.launch {
            when (val result = safeCall {
                ApiClient.api.createHabit(CreateHabitRequest(name, selectedEmoji))
            }) {
                is ApiResult.Success -> {
                    b.habitInput.text.clear()
                    loadHabits()
                }
                is ApiResult.Error -> handleError(result)
            }
            b.addButton.isEnabled = true
        }
    }

    private fun toggle(habit: Habit, day: LocalDate) {
        lifecycleScope.launch {
            when (val result = safeCall {
                ApiClient.api.toggle(habit.id, ToggleRequest(day.toString()))
            }) {
                is ApiResult.Success -> loadHabits()
                is ApiResult.Error -> handleError(result)
            }
        }
    }

    private fun confirmDelete(habit: Habit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete habit?")
            .setMessage("\"${habit.name}\" and its history will be removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    when (val result = safeCall { ApiClient.api.deleteHabit(habit.id) }) {
                        is ApiResult.Success -> loadHabits()
                        is ApiResult.Error -> handleError(result)
                    }
                }
            }
            .show()
    }

    private fun pickEmoji() {
        val grid = GridLayout(this).apply {
            columnCount = 4
            setPadding(dp(16), dp(12), dp(16), dp(4))
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Pick an emoji")
            .setView(grid)
            .setNegativeButton("Cancel", null)
            .create()

        emojiChoices.forEach { emoji ->
            val cell = TextView(this).apply {
                text = emoji
                textSize = 28f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setOnClickListener {
                    selectedEmoji = emoji
                    b.emojiButton.text = emoji
                    dialog.dismiss()
                }
            }
            grid.addView(cell)
        }
        dialog.show()
    }
}

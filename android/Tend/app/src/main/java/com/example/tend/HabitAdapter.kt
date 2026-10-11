package com.example.tend

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tend.databinding.ItemDayBinding
import com.example.tend.databinding.ItemHabitBinding
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class HabitAdapter(
    private val onToggle: (Habit, LocalDate) -> Unit,
    private val onDelete: (Habit) -> Unit
) : RecyclerView.Adapter<HabitAdapter.VH>() {

    private var items: List<Habit> = emptyList()
    private var week: List<LocalDate> = emptyList()
    private var today: LocalDate = LocalDate.now()

    class VH(val b: ItemHabitBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(habits: List<Habit>, week: List<LocalDate>, today: LocalDate) {
        this.items = habits
        this.week = week
        this.today = today
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val habit = items[position]
        val b = holder.b
        val ctx = b.root.context
        val accent = Accent.color(ctx)

        b.emoji.text = habit.emoji
        b.name.text = habit.name
        b.streak.text = habit.streak.toString()
        b.streak.setTextColor(accent)
        b.flame.imageTintList = ColorStateList.valueOf(accent)
        b.deleteButton.setOnClickListener { onDelete(habit) }

        val done = habit.doneDates.toSet()
        val inflater = LayoutInflater.from(ctx)
        val density = ctx.resources.displayMetrics.density

        val ink = ContextCompat.getColor(ctx, R.color.ink)
        val muted = ContextCompat.getColor(ctx, R.color.muted)
        val faded = ContextCompat.getColor(ctx, R.color.switch_off)

        b.weekStrip.removeAllViews()
        for (day in week) {
            val d = ItemDayBinding.inflate(inflater, b.weekStrip, false)
            val isDone = done.contains(day.toString())
            val isFuture = day.isAfter(today)
            val isToday = day == today

            d.letter.text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
            d.number.text = day.dayOfMonth.toString()

            val fill = when {
                isDone -> accent
                isFuture -> ContextCompat.getColor(ctx, R.color.pill_future)
                else -> ContextCompat.getColor(ctx, R.color.pill_bg)
            }
            val pill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f * density
                setColor(fill)
                if (isToday) setStroke((2 * density).toInt(), accent)
            }
            d.root.background = pill

            when {
                isDone -> {
                    d.letter.setTextColor(Color.WHITE)
                    d.number.setTextColor(Color.WHITE)
                }
                isFuture -> {
                    d.letter.setTextColor(faded)
                    d.number.setTextColor(faded)
                }
                else -> {
                    d.letter.setTextColor(muted)
                    d.number.setTextColor(ink)
                }
            }

            if (isFuture) {
                d.root.isClickable = false
            } else {
                d.root.setOnClickListener { onToggle(habit, day) }
            }
            b.weekStrip.addView(d.root)
        }
    }
}

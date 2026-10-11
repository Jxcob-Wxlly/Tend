package com.example.tend

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Schedules the "Daily reminder" notification (every day at 20:00). */
object Reminders {
    const val CHANNEL_ID = "tend_reminder"
    private const val WORK_NAME = "tend_daily_reminder"
    private const val HOUR = 20

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, replace: Boolean) {
        val now = LocalDateTime.now()
        var first = now.toLocalDate().atTime(HOUR, 0)
        if (!first.isAfter(now)) first = first.plusDays(1)
        val delay = Duration.between(now, first).toMillis()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val ctx = applicationContext

        if (!Session.isLoggedIn || !Session.dailyReminder) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(ctx.getString(R.string.reminder_title))
            .setContentText(ctx.getString(R.string.reminder_text))
            .setColor(Accent.color(ctx))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(ctx).notify(1, notification)
        return Result.success()
    }
}

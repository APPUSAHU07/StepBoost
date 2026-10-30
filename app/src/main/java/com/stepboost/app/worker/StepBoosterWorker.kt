package com.stepboost.app.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stepboost.app.R
import com.stepboost.app.StepBoostApplication
import com.stepboost.app.data.HealthConnectManager
import com.stepboost.app.data.StepDistributionEngine
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Background worker that automatically adds realistic step increments
 * at random intervals during daytime hours if enabled.
 */
class StepBoosterWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_AUTO_BOOST_ENABLED, false)

        if (!isEnabled) {
            return Result.success()
        }

        // Only add steps during active day hours (e.g., 08:00 to 22:00)
        val currentHour = LocalTime.now().hour
        val startHour = prefs.getInt(KEY_START_HOUR, 8)
        val endHour = prefs.getInt(KEY_END_HOUR, 21)

        if (currentHour < startHour || currentHour > endHour) {
            // Quiet nighttime hours, skip adding steps
            return Result.success()
        }

        val healthManager = HealthConnectManager(applicationContext)
        if (!healthManager.hasAllPermissions()) {
            return Result.failure()
        }

        // Check if daily target has already been reached
        val currentSummary = healthManager.getTodayStepSummary()
        val dailyGoal = prefs.getLong(KEY_DAILY_TARGET, 10000L)

        if (currentSummary.totalSteps >= dailyGoal) {
            // Already hit the target!
            return Result.success()
        }

        // Pick a natural burst of steps (e.g., 300 to 750 steps)
        val minBurst = prefs.getInt(KEY_MIN_BURST, 250)
        val maxBurst = prefs.getInt(KEY_MAX_BURST, 650)
        val stepsToAdd = Random.nextInt(minBurst, maxBurst).toLong()

        val record = StepDistributionEngine.generateRecentWalk(stepsToAdd)
        val insertResult = healthManager.insertSteps(listOf(record))

        if (insertResult.isSuccess) {
            val showNotifs = prefs.getBoolean(KEY_SHOW_NOTIFICATIONS, true)
            if (showNotifs) {
                notifyUser(stepsToAdd, currentSummary.totalSteps + stepsToAdd)
            }
            return Result.success()
        }

        return Result.retry()
    }

    private fun notifyUser(addedSteps: Long, totalSteps: Long) {
        val notification = NotificationCompat.Builder(applicationContext, StepBoostApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚶‍♂️ Steps Boosted")
            .setContentText("Added $addedSteps steps. Today's total: $totalSteps")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val PREFS_NAME = "step_boost_prefs"
        const val KEY_AUTO_BOOST_ENABLED = "auto_boost_enabled"
        const val KEY_DAILY_TARGET = "daily_target"
        const val KEY_MIN_BURST = "min_burst"
        const val KEY_MAX_BURST = "max_burst"
        const val KEY_START_HOUR = "start_hour"
        const val KEY_END_HOUR = "end_hour"
        const val KEY_SHOW_NOTIFICATIONS = "show_notifications"
        const val KEY_INTERVAL_HOURS = "interval_hours"

        private const val WORK_NAME = "StepBoosterPeriodicWork"
        private const val NOTIFICATION_ID = 2001

        fun schedule(context: Context, intervalHours: Long = 1) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<StepBoosterWorker>(
                intervalHours.coerceAtLeast(1),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

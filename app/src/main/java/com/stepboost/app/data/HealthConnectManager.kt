package com.stepboost.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

sealed class HealthConnectStatus {
    object Available : HealthConnectStatus()
    object UpdateRequired : HealthConnectStatus()
    object NotSupported : HealthConnectStatus()
}

data class DailyStepSummary(
    val totalSteps: Long = 0,
    val recordsCount: Int = 0,
    val records: List<StepsRecord> = emptyList()
)

class HealthConnectManager(private val context: Context) {

    val healthConnectClient: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    fun checkAvailability(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UpdateRequired
            else -> HealthConnectStatus.NotSupported
        }
    }

    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        val granted = client.permissionController.getGrantedPermissions()
        granted.containsAll(permissions)
    }

    /**
     * Reads the total aggregated step count for today (00:00 to now).
     */
    suspend fun getTodayStepSummary(zoneId: ZoneId = ZoneId.systemDefault()): DailyStepSummary = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext DailyStepSummary()

        val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
        val now = Instant.now()

        val timeRange = TimeRangeFilter.between(startOfDay, now)

        try {
            // Read aggregate total
            val aggregateResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange
                )
            )
            val total = aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L

            // Read individual records for today
            val recordsResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange
                )
            )

            DailyStepSummary(
                totalSteps = total,
                recordsCount = recordsResponse.records.size,
                records = recordsResponse.records.sortedByDescending { it.startTime }
            )
        } catch (e: Exception) {
            DailyStepSummary()
        }
    }

    /**
     * Inserts a list of StepsRecord into Health Connect.
     */
    suspend fun insertSteps(records: List<StepsRecord>): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(IllegalStateException("Health Connect is unavailable"))
        try {
            client.insertRecords(records)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quick boost: Adds steps right now as a realistic recent walk.
     */
    suspend fun addQuickWalk(steps: Long): Result<Unit> {
        val record = StepDistributionEngine.generateRecentWalk(steps)
        return insertSteps(listOf(record))
    }

    /**
     * Boosts today's steps up to a target number or adds a target quantity
     * distributed realistically across today's active hours (e.g. 8:00 AM to now).
     */
    suspend fun boostRealisticSteps(
        stepsToAdd: Long,
        startHour: Int = 8,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Result<Unit> {
        val now = ZonedDateTime.now(zoneId)
        val todayStart = LocalDate.now(zoneId).atTime(startHour, 0).atZone(zoneId)

        val windowStart = if (now.isBefore(todayStart)) now.minusHours(2) else todayStart
        val windowEnd = now

        val records = StepDistributionEngine.distributeStepsAcrossWindow(
            totalSteps = stepsToAdd,
            windowStart = windowStart,
            windowEnd = windowEnd,
            zoneId = zoneId
        )

        return insertSteps(records)
    }

    /**
     * Deletes step records by their IDs.
     */
    suspend fun deleteRecords(recordIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(IllegalStateException("Health Connect is unavailable"))
        try {
            client.deleteRecords(
                recordType = StepsRecord::class,
                recordIdsList = recordIds,
                clientRecordIdsList = emptyList()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

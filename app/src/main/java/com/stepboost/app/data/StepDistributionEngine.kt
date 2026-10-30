package com.stepboost.app.data

import androidx.health.connect.client.records.StepsRecord
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Engine that generates natural, human-like step distributions across time intervals.
 * 
 * Humans walk with an average cadence of 100-125 steps per minute.
 * Rather than inserting 10,000 steps in a single unrealistic burst, this engine
 * splits the target steps into realistic sessions throughout the active hours of the day.
 */
object StepDistributionEngine {

    private const val MIN_CADENCE = 95.0 // steps per minute (casual stroll)
    private const val MAX_CADENCE = 125.0 // steps per minute (brisk walking)

    /**
     * Generates a single natural walking burst ending at the current time or a specified instant.
     * Ideal for quick boosts (e.g. +500 or +1000 steps).
     */
    fun generateRecentWalk(
        stepCount: Long,
        endTime: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StepsRecord {
        val cadence = Random.nextDouble(MIN_CADENCE, MAX_CADENCE)
        val durationMinutes = (stepCount / cadence).coerceAtLeast(1.0)
        val durationSeconds = (durationMinutes * 60).toLong()
        val startTime = endTime.minus(durationSeconds, ChronoUnit.SECONDS)

        val zoneOffset = zoneId.rules.getOffset(startTime)
        val endOffset = zoneId.rules.getOffset(endTime)

        return StepsRecord(
            count = stepCount,
            startTime = startTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = endOffset
        )
    }

    /**
     * Distributes a larger amount of steps (e.g. 3,000 - 15,000 steps) across a day or time window.
     * Divides the total steps into 3 to 10 natural walking sessions (e.g., morning walk, lunch stroll,
     * afternoon breaks, evening walk), each with randomized realistic cadences and duration.
     */
    fun distributeStepsAcrossWindow(
        totalSteps: Long,
        windowStart: ZonedDateTime,
        windowEnd: ZonedDateTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<StepsRecord> {
        if (totalSteps <= 0) return emptyList()
        if (!windowEnd.isAfter(windowStart)) return emptyList()

        val totalWindowMinutes = ChronoUnit.MINUTES.between(windowStart, windowEnd)
        if (totalWindowMinutes < 15) {
            // Window is very narrow, generate single burst
            return listOf(generateRecentWalk(totalSteps, windowEnd.toInstant(), zoneId))
        }

        // Determine number of sessions based on step count
        val sessionCount = when {
            totalSteps < 1000 -> 1
            totalSteps < 3000 -> Random.nextInt(2, 4)
            totalSteps < 6000 -> Random.nextInt(3, 6)
            totalSteps < 10000 -> Random.nextInt(5, 8)
            else -> Random.nextInt(6, 11)
        }

        // Partition total steps across sessions with natural variance
        val stepAllocations = partitionSteps(totalSteps, sessionCount)
        val records = mutableListOf<StepsRecord>()

        // Divide the day window into slots to evenly space walking sessions
        val slotMinutes = totalWindowMinutes / sessionCount

        for (i in 0 until sessionCount) {
            val stepsForSession = stepAllocations[i]
            if (stepsForSession <= 0) continue

            val cadence = Random.nextDouble(MIN_CADENCE, MAX_CADENCE)
            val walkDurationMinutes = (stepsForSession / cadence).coerceIn(2.0, (slotMinutes * 0.8).coerceAtLeast(3.0))

            // Choose a start time within this slot
            val slotStartTime = windowStart.plusMinutes(i * slotMinutes)
            val maxOffsetInSlot = (slotMinutes - walkDurationMinutes).toInt().coerceAtLeast(1)
            val randomOffset = Random.nextInt(0, maxOffsetInSlot)

            val sessionStart = slotStartTime.plusMinutes(randomOffset.toLong())
            val sessionEnd = sessionStart.plusSeconds((walkDurationMinutes * 60).toLong())

            // Ensure we don't exceed windowEnd
            val finalStart = if (sessionEnd.isAfter(windowEnd)) windowEnd.minusSeconds((walkDurationMinutes * 60).toLong()) else sessionStart
            val finalEnd = if (sessionEnd.isAfter(windowEnd)) windowEnd else sessionEnd

            val startInstant = finalStart.toInstant()
            val endInstant = finalEnd.toInstant()

            records.add(
                StepsRecord(
                    count = stepsForSession,
                    startTime = startInstant,
                    startZoneOffset = zoneId.rules.getOffset(startInstant),
                    endTime = endInstant,
                    endZoneOffset = zoneId.rules.getOffset(endInstant)
                )
            )
        }

        return records
    }

    private fun partitionSteps(totalSteps: Long, parts: Int): List<Long> {
        if (parts <= 1) return listOf(totalSteps)

        // Generate random weights
        val weights = List(parts) { Random.nextDouble(0.6, 1.4) }
        val sumWeights = weights.sum()

        var remaining = totalSteps
        val allocations = mutableListOf<Long>()

        for (i in 0 until parts - 1) {
            val count = ((weights[i] / sumWeights) * totalSteps).toLong().coerceAtLeast(50L)
            allocations.add(count)
            remaining -= count
        }
        allocations.add(remaining.coerceAtLeast(50L))
        return allocations
    }
}

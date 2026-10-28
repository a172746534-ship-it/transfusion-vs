package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointType.HOURLY
import app.transfusionvs.data.CheckpointType.LIMIT_WARNING
import app.transfusionvs.data.CheckpointType.M15
import app.transfusionvs.data.CheckpointType.M30
import app.transfusionvs.data.CheckpointType.PLANNED_END
import app.transfusionvs.data.CheckpointType.START

// Pure Kotlin, no Android: everything here is covered by ScheduleCalculatorTest.
// All schedule math is real elapsed time from T0 (epoch millis). The clock time shown to the
// user is derived later with java.time, so a timezone change only changes the display.

const val MINUTE_MS = 60_000L

data class ScheduleConfig(
    val plannedDurationMin: Int,
    val limitMin: Int = 240,
    val warnBeforeMin: Int = 15,
)

data class PlannedCheckpoint(val type: CheckpointType, val offsetMin: Int)

object ScheduleCalculator {

    /** How far past "now" hourly checkpoints are created in advance (extended on every re-arm). */
    const val LOOKAHEAD_MIN = 180

    fun truncateToMinute(epochMs: Long): Long = epochMs - Math.floorMod(epochMs, MINUTE_MS)

    fun timeOf(t0: Long, offsetMin: Int): Long = t0 + offsetMin * MINUTE_MS

    /**
     * The card's schedule: START at T0 (logged, no alarm), T0+15, T0+30, then every 60 min at
     * the same minute as T0, up to [untilOffsetMin]. The End check replaces whichever fixed
     * check falls on the same minute; a custom duration gets its own End check. Hourly checks
     * continue past the planned end until the nurse taps End.
     */
    fun plan(config: ScheduleConfig, untilOffsetMin: Int): List<PlannedCheckpoint> {
        val out = mutableListOf(PlannedCheckpoint(START, 0))
        if (untilOffsetMin >= 15) out += PlannedCheckpoint(M15, 15)
        if (untilOffsetMin >= 30) out += PlannedCheckpoint(M30, 30)
        var h = 60
        while (h <= untilOffsetMin) {
            out += PlannedCheckpoint(HOURLY, h)
            h += 60
        }

        val end = config.plannedDurationMin
        if (end in 1..untilOffsetMin) {
            out.removeAll { it.offsetMin == end && it.type != START }
            out += PlannedCheckpoint(PLANNED_END, end)
        }

        val warnAt = config.limitMin - config.warnBeforeMin
        if (config.warnBeforeMin > 0 && warnAt in 1..untilOffsetMin) {
            out += PlannedCheckpoint(LIMIT_WARNING, warnAt)
        }
        return out.sortedWith(compareBy<PlannedCheckpoint> { it.offsetMin }.thenBy { it.type.ordinal })
    }

    /** Checkpoints are created through the planned end and the time limit, and always
     * [LOOKAHEAD_MIN] past now, so a transfusion that overruns keeps getting hourly alarms. */
    fun horizonOffsetMin(t0: Long, config: ScheduleConfig, now: Long): Int {
        val elapsedMin = ((now - t0) / MINUTE_MS).toInt().coerceAtLeast(0)
        return maxOf(config.plannedDurationMin, config.limitMin, elapsedMin + LOOKAHEAD_MIN)
    }
}

/** Matches a fresh plan against the rows already stored (after a start, an edit, or a re-arm). */
object Reconciler {

    data class Result(
        val insert: List<Checkpoint>,
        val update: List<Checkpoint>,
        val delete: List<Long>,
    )

    // M15/M30/HOURLY/PLANNED_END form one series (one per minute); START and LIMIT_WARNING
    // are their own. Matching by series lets a duration edit turn an End check back into an
    // hourly check (and vice versa) instead of creating a second alarm on the same minute.
    private enum class Series { START, FIXED, LIMIT, OTHER }

    private fun seriesOf(type: CheckpointType) = when (type) {
        START -> Series.START
        M15, M30, HOURLY, PLANNED_END -> Series.FIXED
        LIMIT_WARNING -> Series.LIMIT
        else -> Series.OTHER
    }

    /**
     * Done rows (acknowledged, missed, ringing, cancelled) are history and never touched.
     * Pending rows are moved to their new time or type, or deleted if no longer planned.
     * New rows are inserted as PENDING — if their time has already passed, the alarm engine
     * rings them together as one immediate alarm. START is logged as done when it is created.
     */
    fun reconcile(
        transfusionId: Long,
        t0: Long,
        existing: List<Checkpoint>,
        planned: List<PlannedCheckpoint>,
        now: Long,
    ): Result {
        val stored = existing.filter { seriesOf(it.type) != Series.OTHER }
        val byKey = stored.associateBy { seriesOf(it.type) to it.offsetMin }
        val plannedKeys = planned.map { seriesOf(it.type) to it.offsetMin }.toSet()

        val insert = mutableListOf<Checkpoint>()
        val update = mutableListOf<Checkpoint>()
        for (p in planned) {
            val at = ScheduleCalculator.timeOf(t0, p.offsetMin)
            val row = byKey[seriesOf(p.type) to p.offsetMin]
            if (row == null) {
                insert += if (p.type == START) {
                    Checkpoint(
                        transfusionId = transfusionId, type = START, offsetMin = 0,
                        scheduledAt = at, ackAt = now, status = CheckpointStatus.ACKED,
                    )
                } else {
                    Checkpoint(transfusionId = transfusionId, type = p.type, offsetMin = p.offsetMin, scheduledAt = at)
                }
            } else if (row.type == START) {
                if (row.scheduledAt != at) update += row.copy(scheduledAt = at)
            } else if (row.status == CheckpointStatus.PENDING && (row.scheduledAt != at || row.type != p.type)) {
                update += row.copy(scheduledAt = at, type = p.type)
            }
        }
        val delete = stored
            .filter { it.status == CheckpointStatus.PENDING && (seriesOf(it.type) to it.offsetMin) !in plannedKeys }
            .map { it.id }
        return Result(insert, update, delete)
    }
}

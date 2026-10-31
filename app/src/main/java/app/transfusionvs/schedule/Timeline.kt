package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.Transfusion

// Pure models behind the card-style timeline and the per-checkpoint status (unit tested).

/** Done, still to come, due now, overdue, and a turn that was skipped. */
enum class DotStyle { FILLED, HOLLOW, DUE, OVERDUE, SKIPPED }

/** One column of the card's "TIME FROM START" row: "15 min" over a dot over a time chip. */
data class TimelinePoint(
    val number: String,
    val unit: String?,
    val time: Long,
    /** Navy-filled chip for start and hourly; outlined for 15 min, 30 min and End. */
    val filledChip: Boolean,
    val dot: DotStyle,
    /** Repositioning: the position for this slot, shown as an icon instead of a number. */
    val position: Position? = null,
)

object TimelineBuilder {

    /** Before Start: the planned row through the planned End, like the card's example row. */
    fun preview(t0: Long, config: ScheduleConfig): List<TimelinePoint> =
        ScheduleCalculator.plan(config, config.plannedDurationMin)
            .filter { it.type != CheckpointType.LIMIT_WARNING }
            .map { p ->
                val dot = if (p.type == CheckpointType.START) DotStyle.FILLED else DotStyle.HOLLOW
                point(p.type, p.offsetMin, ScheduleCalculator.timeOf(t0, p.offsetMin), dot)
            }

    /**
     * A started transfusion. Shows the plan through the planned End; once it runs past the End,
     * the next hourly checks appear too. Dots: filled = start / done, hollow = upcoming,
     * due = ringing or due and not yet rung, overdue = missed.
     */
    fun live(t: Transfusion, rows: List<Checkpoint>, now: Long): List<TimelinePoint> =
        rows.asSequence()
            .filter { it.type in CARD_TYPES && it.status != CheckpointStatus.CANCELLED }
            .filter { it.offsetMin <= t.plannedDurationMin || it.status != CheckpointStatus.PENDING || it.scheduledAt <= now + 60 * MINUTE_MS }
            .sortedBy { it.offsetMin }
            .map { point(it.type, it.offsetMin, it.scheduledAt, dotFor(it, now)) }
            .toList()

    private val CARD_TYPES = setOf(
        CheckpointType.START, CheckpointType.M15, CheckpointType.M30, CheckpointType.HOURLY, CheckpointType.PLANNED_END,
    )

    private fun dotFor(c: Checkpoint, now: Long): DotStyle = when {
        c.type == CheckpointType.START -> DotStyle.FILLED
        c.status == CheckpointStatus.ACKED -> DotStyle.FILLED
        c.status == CheckpointStatus.MISSED -> DotStyle.OVERDUE
        c.status == CheckpointStatus.RINGING -> DotStyle.DUE
        c.scheduledAt <= now -> DotStyle.DUE
        else -> DotStyle.HOLLOW
    }

    private fun point(type: CheckpointType, offsetMin: Int, time: Long, dot: DotStyle) = when (type) {
        CheckpointType.START -> TimelinePoint("0", null, time, filledChip = true, dot = dot)
        CheckpointType.M15 -> TimelinePoint("15", "min", time, filledChip = false, dot = dot)
        CheckpointType.M30 -> TimelinePoint("30", "min", time, filledChip = false, dot = dot)
        CheckpointType.HOURLY -> TimelinePoint("${offsetMin / 60}", "h", time, filledChip = true, dot = dot)
        else -> TimelinePoint("End", null, time, filledChip = false, dot = dot)
    }
}

enum class StatusKind { UPCOMING, DUE, RINGING, SNOOZED, ON_TIME, LATE, MISSED, SKIPPED, CANCELLED }

data class StatusView(val kind: StatusKind, val text: String)

object Timeliness {

    /** Minutes from the scheduled time to "Measured & documented". */
    fun delayMin(c: Checkpoint): Long? = c.ackAt?.let { Math.floorDiv(it - c.scheduledAt, MINUTE_MS) }

    /** upcoming / ringing / on time / late +N min / missed — as on the Detail screen. */
    fun status(c: Checkpoint, now: Long, toleranceMin: Int): StatusView = when (c.status) {
        CheckpointStatus.PENDING -> when {
            c.type == CheckpointType.END -> StatusView(StatusKind.DUE, "to document")
            c.scheduledAt <= now -> StatusView(StatusKind.DUE, "due now")
            else -> StatusView(StatusKind.UPCOMING, "upcoming")
        }
        CheckpointStatus.RINGING ->
            if (c.snoozeUntil != null && c.snoozeUntil > now) StatusView(StatusKind.SNOOZED, "snoozed")
            else StatusView(StatusKind.RINGING, "ringing")
        CheckpointStatus.ACKED -> {
            val d = delayMin(c) ?: 0
            if (d <= toleranceMin) StatusView(StatusKind.ON_TIME, "on time") else StatusView(StatusKind.LATE, "late +$d min")
        }
        CheckpointStatus.MISSED -> {
            val d = delayMin(c)
            StatusView(StatusKind.MISSED, if (d == null) "overdue" else "missed · documented +$d min")
        }
        CheckpointStatus.CANCELLED -> StatusView(StatusKind.CANCELLED, "cancelled")
    }

    /** "in 12 min", "in 1 h 05 min", "now". */
    fun countdown(msUntil: Long): String {
        if (msUntil <= 30_000) return "now"
        val totalMin = (msUntil + MINUTE_MS - 1) / MINUTE_MS // round up: never say "in 0 min"
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h == 0L) "in $m min" else "in $h h ${m.toString().padStart(2, '0')} min"
    }
}

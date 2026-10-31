package app.transfusionvs.schedule

import app.transfusionvs.data.Position
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import java.time.ZoneId

// Pure view models for repositioning: the same timeline points and status pills as transfusions.

object TurnTimeline {

    /**
     * Before Start: "now" with the current position, the next slots with the position each turn
     * should reach, and the stop time (outlined, like a transfusion's End).
     */
    fun preview(
        start: Long,
        current: Position,
        rotation: List<Position>,
        firstSlot: Long,
        intervalMin: Int,
        stopAt: Long?,
        zone: ZoneId,
        slotCount: Int = 5,
    ): List<TimelinePoint> {
        val out = mutableListOf(TimelinePoint("", null, start, filledChip = true, dot = DotStyle.FILLED, position = current))
        var position = current
        var slot = firstSlot
        repeat(slotCount) {
            if (stopAt != null && slot >= stopAt) return@repeat
            position = TurnGrid.nextPosition(position, rotation)
            out += TimelinePoint("", null, slot, filledChip = true, dot = DotStyle.HOLLOW, position = position)
            slot = TurnGrid.nextSlotAfter(slot, intervalMin, zone)
        }
        if (stopAt != null && out.last().time < stopAt && stopAt <= slot) {
            out += TimelinePoint("Stop", null, stopAt, filledChip = false, dot = DotStyle.HOLLOW)
        }
        return out
    }

    /**
     * A running plan: the last two finished slots, anything open, and the next upcoming ones.
     * Dots: filled = done, dash = skipped, due = ringing, overdue = missed, hollow = upcoming.
     */
    fun live(plan: TurnPlan, slots: List<TurnSlot>, now: Long, ahead: Int = 3): List<TimelinePoint> {
        val sorted = slots.filter { it.status != TurnSlotStatus.CANCELLED }.sortedBy { it.slotAt }
        val done = sorted.filter { it.status in FINISHED }.takeLast(2)
        val open = sorted.filter { it.status == TurnSlotStatus.RINGING || (it.status == TurnSlotStatus.PENDING && it.slotAt <= now) }
        val upcoming = sorted.filter { it.status == TurnSlotStatus.PENDING && it.slotAt > now }.take(ahead)

        val rotation = Position.parseList(plan.rotation)
        var predicted = plan.currentPosition
        val points = (done + open + upcoming).map { s ->
            val position = when (s.status) {
                TurnSlotStatus.ACKED -> s.positionTo ?: s.positionFrom
                TurnSlotStatus.MISSED, TurnSlotStatus.SKIPPED -> s.positionFrom
                else -> TurnGrid.nextPosition(predicted, rotation).also { predicted = it }
            }
            val dot = when {
                s.status == TurnSlotStatus.ACKED -> DotStyle.FILLED
                s.status == TurnSlotStatus.SKIPPED -> DotStyle.SKIPPED
                s.status == TurnSlotStatus.MISSED -> DotStyle.OVERDUE
                s.status == TurnSlotStatus.RINGING || s.slotAt <= now -> DotStyle.DUE
                else -> DotStyle.HOLLOW
            }
            TimelinePoint("", null, s.slotAt, filledChip = true, dot = dot, position = position)
        }
        val stop = plan.stopAt
        return if (stop != null && (points.isEmpty() || points.last().time < stop) && upcoming.size < ahead) {
            points + TimelinePoint("Stop", null, stop, filledChip = false, dot = DotStyle.HOLLOW)
        } else {
            points
        }
    }

    private val FINISHED = setOf(TurnSlotStatus.ACKED, TurnSlotStatus.MISSED, TurnSlotStatus.SKIPPED)
}

object TurnStatus {

    /** on time / late +N min / missed / skipped (reason) / ringing / upcoming. */
    fun of(s: TurnSlot, now: Long, windowMin: Int, missedTimeoutMin: Int = 10): StatusView = when (s.status) {
        TurnSlotStatus.PENDING ->
            if (s.slotAt <= now) StatusView(StatusKind.DUE, "due now") else StatusView(StatusKind.UPCOMING, "upcoming")
        TurnSlotStatus.RINGING -> when {
            TurnRules.isOverdue(s, now, missedTimeoutMin) -> StatusView(StatusKind.MISSED, "overdue")
            s.silenced -> StatusView(StatusKind.DUE, "round open")
            s.snoozeUntil != null && s.snoozeUntil > now -> StatusView(StatusKind.SNOOZED, "snoozed")
            else -> StatusView(StatusKind.RINGING, "ringing")
        }
        TurnSlotStatus.ACKED -> {
            val late = Math.floorDiv((s.ackAt ?: s.slotAt) - s.slotAt, MINUTE_MS)
            if (late <= windowMin) StatusView(StatusKind.ON_TIME, "on time") else StatusView(StatusKind.LATE, "late +$late min")
        }
        TurnSlotStatus.MISSED -> StatusView(StatusKind.MISSED, "missed")
        TurnSlotStatus.SKIPPED -> StatusView(StatusKind.SKIPPED, "skipped · ${s.skipReason?.label ?: "Other"}")
        TurnSlotStatus.CANCELLED -> StatusView(StatusKind.CANCELLED, "cancelled")
    }

    /** "1 h 55 min" — for the longest gap without a turn. */
    fun gap(ms: Long): String {
        val totalMin = ms / MINUTE_MS
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h == 0L) "$m min" else "$h h ${m.toString().padStart(2, '0')} min"
    }
}

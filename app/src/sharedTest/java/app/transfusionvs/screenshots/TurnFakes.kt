package app.transfusionvs.screenshots

import app.transfusionvs.alarm.RoundBed
import app.transfusionvs.alarm.RoundUi
import app.transfusionvs.data.Position
import app.transfusionvs.data.Position.BACK
import app.transfusionvs.data.Position.LEFT_30
import app.transfusionvs.data.Position.RIGHT_30
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.screenshots.Fakes.at

/** Repositioning sample data, same day and "now" (11:05) as [Fakes]. */
object TurnFakes {

    private var nextId = 500L

    private fun plan(
        id: Long, bed: String, color: Int, interval: Int, start: String, first: String, stop: String?,
        rotation: List<Position>, current: Position,
    ) = TurnPlan(
        id = id, bedLabel = bed, colorIndex = color, intervalMin = interval, startAt = at(start), firstSlotAt = at(first),
        stopAt = stop?.let { at(it) }, rotation = Position.toCsv(rotation), currentPosition = current,
    )

    private fun slot(
        planId: Long, time: String, from: Position, status: TurnSlotStatus = TurnSlotStatus.PENDING,
        to: Position? = null, ack: String? = null, reason: SkipReason? = null, silenced: Boolean = false,
    ) = TurnSlot(
        id = nextId++, planId = planId, slotAt = at(time), firedAt = if (status == TurnSlotStatus.PENDING) null else at(time),
        ackAt = ack?.let { at(it) }, positionFrom = from, positionTo = to, status = status, skipReason = reason, silenced = silenced,
    )

    /** 7654803: turned on time at 08:00, late at 10:25 — the grid still says 12:00 next. */
    val bed3 = TurnPlanWithSlots(
        plan(31, "7654803", 2, 120, "2026-01-15T07:30", "2026-01-15T08:00", "2026-01-15T20:00", listOf(LEFT_30, BACK, RIGHT_30), BACK),
        listOf(
            slot(31, "2026-01-15T08:00", RIGHT_30, TurnSlotStatus.ACKED, LEFT_30, ack = "2026-01-15T08:06"),
            slot(31, "2026-01-15T10:00", LEFT_30, TurnSlotStatus.ACKED, BACK, ack = "2026-01-15T10:25"),
            slot(31, "2026-01-15T12:00", BACK),
            slot(31, "2026-01-15T14:00", BACK),
            slot(31, "2026-01-15T16:00", BACK),
        ),
    )

    /** 7654807: missed 08:00, 10:00 still not done (Overdue). */
    val bed7 = TurnPlanWithSlots(
        plan(32, "7654807", 3, 120, "2026-01-15T07:00", "2026-01-15T08:00", null, listOf(LEFT_30, BACK, RIGHT_30), LEFT_30),
        listOf(
            slot(32, "2026-01-15T08:00", LEFT_30, TurnSlotStatus.MISSED),
            slot(32, "2026-01-15T10:00", LEFT_30, TurnSlotStatus.RINGING, silenced = true),
            slot(32, "2026-01-15T12:00", LEFT_30),
        ),
    )

    /** A long bed label, 4-hourly, left side excluded (line on that side), skipped once. */
    val isolation = TurnPlanWithSlots(
        plan(33, "7654800123", 4, 240, "2026-01-15T06:40", "2026-01-15T08:00", "2026-01-15T20:00", listOf(BACK, RIGHT_30), RIGHT_30),
        listOf(
            slot(33, "2026-01-15T08:00", RIGHT_30, TurnSlotStatus.SKIPPED, reason = SkipReason.PROCEDURE),
            slot(33, "2026-01-15T12:00", RIGHT_30),
            slot(33, "2026-01-15T16:00", RIGHT_30),
        ),
    )

    val home = listOf(bed3, bed7, isolation)

    private fun roundBed(i: Int, bed: String, from: Position, to: Position, allowed: List<Position> = listOf(LEFT_30, BACK, RIGHT_30)) =
        RoundBed(planId = 40L + i, slotId = 60L + i, bedLabel = bed, colorIndex = i, from = from, suggested = to, allowed = allowed)

    val roundBeds = listOf(
        roundBed(0, "7654803", LEFT_30, BACK),
        roundBed(1, "7654807", BACK, RIGHT_30),
        roundBed(2, "7654809", RIGHT_30, LEFT_30),
        roundBed(3, "7654800123", BACK, RIGHT_30, allowed = listOf(BACK, RIGHT_30)),
    )

    fun round(beds: List<RoundBed> = roundBeds, started: Boolean = false) = RoundUi(
        slotAt = at("2026-01-15T10:00"), beds = beds, started = started, canSnooze = !started, snoozedUntil = null,
        canRemind = started,
    )

    /** Seven beds on one slot: every label must fit. */
    val sevenBeds = (0 until 7).map { i ->
        roundBed(i, if (i == 6) "7654800123" else "76548${(i + 1).toString().padStart(2, '0')}", listOf(LEFT_30, BACK, RIGHT_30)[i % 3], listOf(BACK, RIGHT_30, LEFT_30)[i % 3])
    }
}

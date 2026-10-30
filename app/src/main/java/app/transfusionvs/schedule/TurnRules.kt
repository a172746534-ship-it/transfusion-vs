package app.transfusionvs.schedule

import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import java.time.ZoneId

// Pure state rules for repositioning slots (unit tested in TurnRulesTest). The engine applies them
// alongside the transfusion rules and arms everything through the same AlarmScheduler.
//
// A slot: PENDING → RINGING (sound) → silenced at the missed timer (sound stops, Overdue shows,
// still ackable) → MISSED when the next slot fires. Done = ACKED (turned) or SKIPPED (can't turn).

object TurnRules {

    /** Slots are created this far ahead (and re-extended on every refresh). */
    const val LOOKAHEAD_MS = 6 * 60 * MINUTE_MS

    /** Finished slots are kept this long (the phone keeps nothing long-term). */
    const val KEEP_MS = 24 * 60 * MINUTE_MS

    /**
     * Upcoming slots no longer on the local grid (the phone's timezone changed): they are removed
     * and [materialize] recreates the right ones, because the ward clock follows the local time.
     */
    fun offGridPending(plan: TurnPlan, slots: List<TurnSlot>, now: Long, zone: ZoneId): List<TurnSlot> {
        if (plan.status != TurnPlanStatus.RUNNING) return emptyList()
        val grid = TurnGrid.slots(plan.firstSlotAt, plan.intervalMin, plan.stopAt, now + LOOKAHEAD_MS, zone).toSet()
        return slots.filter { it.status == TurnSlotStatus.PENDING && it.slotAt > now && it.slotAt !in grid }
    }

    /**
     * New slot rows for a running plan up to now + lookahead, never at or after the stop time.
     * Slots already closed by the time they are created (app was off) are created as MISSED.
     */
    fun materialize(plan: TurnPlan, existing: List<TurnSlot>, now: Long, zone: ZoneId): List<TurnSlot> {
        if (plan.status != TurnPlanStatus.RUNNING) return emptyList()
        val have = existing.map { it.slotAt }.toSet()
        return TurnGrid.slots(plan.firstSlotAt, plan.intervalMin, plan.stopAt, now + LOOKAHEAD_MS, zone)
            // Older slots were cleaned up on purpose: never recreate them (as false "missed").
            .filter { it !in have && it >= now - KEEP_MS }
            .map { at ->
                val closed = TurnGrid.missAt(at, plan.intervalMin, zone) <= now
                TurnSlot(
                    planId = plan.id, slotAt = at, positionFrom = plan.currentPosition,
                    status = if (closed) TurnSlotStatus.MISSED else TurnSlotStatus.PENDING,
                )
            }
    }

    /** One refresh step at [now]: returns only the slots that changed. */
    fun step(plan: TurnPlan, slots: List<TurnSlot>, now: Long, missedTimeoutMin: Int, zone: ZoneId): List<TurnSlot> =
        slots.mapNotNull { s ->
            val open = s.status == TurnSlotStatus.RINGING || (s.status == TurnSlotStatus.PENDING && s.slotAt <= now + 1_000)
            if (!open) return@mapNotNull null
            val next = when {
                // The next slot fired: this one closes as MISSED (a late ack was possible until now).
                TurnGrid.missAt(s.slotAt, plan.intervalMin, zone) <= now ->
                    s.copy(status = TurnSlotStatus.MISSED, snoozeUntil = null, remindAt = null)
                // Due: ring, from where the patient lies now (any bed due this minute rings together).
                s.status == TurnSlotStatus.PENDING ->
                    s.copy(status = TurnSlotStatus.RINGING, firedAt = now, positionFrom = plan.currentPosition)
                // "Remind me in 30 min" came due: ring again, with a fresh missed timer.
                s.remindAt != null && s.remindAt <= now ->
                    s.copy(silenced = false, remindAt = null, firedAt = now, snoozeUntil = null)
                // Missed timer: stop the sound; the slot stays open (Overdue) until the next slot.
                !s.silenced && (s.firedAt ?: now) + missedTimeoutMin * MINUTE_MS <= now ->
                    s.copy(silenced = true, snoozeUntil = null)
                s.snoozeUntil != null && s.snoozeUntil <= now -> s.copy(snoozeUntil = null)
                else -> s
            }
            next.takeIf { it != s }
        }

    /** Plays sound now: ringing, not silenced, not snoozed. */
    fun isAudible(s: TurnSlot, now: Long): Boolean =
        s.status == TurnSlotStatus.RINGING && !s.silenced && (s.snoozeUntil == null || s.snoozeUntil <= now)

    /** Open past the missed timer: listed on the Overdue notification. */
    fun isOverdue(s: TurnSlot, now: Long, missedTimeoutMin: Int): Boolean =
        s.status == TurnSlotStatus.RINGING && (s.firedAt ?: now) + missedTimeoutMin * MINUTE_MS <= now

    /** The system alarms a plan needs. */
    fun arm(plan: TurnPlan, slots: List<TurnSlot>, now: Long, missedTimeoutMin: Int, zone: ZoneId): ArmPlan {
        val requests = mutableListOf<AlarmRequest>()
        var ringNow = false
        for (s in slots) {
            when (s.status) {
                TurnSlotStatus.PENDING ->
                    if (s.slotAt > now) requests += AlarmRequest(AlarmKind.CHECKPOINT, s.id, s.slotAt, turn = true) else ringNow = true
                TurnSlotStatus.RINGING -> {
                    requests += AlarmRequest(AlarmKind.MISS, s.id, TurnGrid.missAt(s.slotAt, plan.intervalMin, zone), turn = true)
                    // Also after "Start round" (silenced): this is what posts the Overdue notification.
                    val timeoutAt = (s.firedAt ?: now) + missedTimeoutMin * MINUTE_MS
                    if (timeoutAt > now) requests += AlarmRequest(AlarmKind.TIMEOUT, s.id, timeoutAt, turn = true)
                    val snooze = s.snoozeUntil
                    val remind = s.remindAt
                    if (snooze != null && snooze > now) requests += AlarmRequest(AlarmKind.SNOOZE, s.id, snooze, turn = true)
                    if (remind != null && remind > now) requests += AlarmRequest(AlarmKind.REMIND, s.id, remind, turn = true)
                    if (isAudible(s, now)) ringNow = true
                }
                else -> Unit
            }
        }
        return ArmPlan(requests, ringNow)
    }

    /** A plan whose stop time has passed and has nothing open is stopped. */
    fun shouldAutoStop(plan: TurnPlan, slots: List<TurnSlot>, now: Long): Boolean {
        val stop = plan.stopAt ?: return false
        return plan.status == TurnPlanStatus.RUNNING && stop <= now &&
            slots.none { it.status == TurnSlotStatus.RINGING || (it.status == TurnSlotStatus.PENDING && it.slotAt < stop) }
    }
}

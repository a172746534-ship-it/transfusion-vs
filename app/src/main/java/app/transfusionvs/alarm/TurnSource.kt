package app.transfusionvs.alarm

import app.transfusionvs.data.Settings
import app.transfusionvs.data.TurnDao
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.schedule.TurnRules
import java.time.ZoneId

/**
 * Repositioning as one more source for the shared alarm engine: it keeps its slots up to date and
 * tells the engine which system alarms to set. Ringing, snooze, ring-now, notifications, re-arm
 * after reboot/update/time change all stay in [Engine] and [AlarmService], shared with transfusions.
 * Called only while the engine holds its lock.
 */
class TurnSource(
    private val dao: TurnDao,
    private val settings: Settings,
    private val scheduler: AlarmScheduler,
) {
    private fun zone() = ZoneId.systemDefault()

    /** Create upcoming slots, ring what is due, silence / close what timed out, auto-stop at the stop time. */
    suspend fun refresh(now: Long) {
        dao.deleteStoppedBefore(now - Engine.RETENTION_MS)
        dao.deleteOldSlots(now - Engine.RETENTION_MS)
        val zone = zone()
        for (plan in dao.allPlans()) {
            var slots = dao.slotsFor(plan.id)
            val offGrid = TurnRules.offGridPending(plan, slots, now, zone)
            if (offGrid.isNotEmpty()) {
                offGrid.forEach { scheduler.cancelTurn(it.id) }
                dao.deleteSlots(offGrid.map { it.id })
                slots = dao.slotsFor(plan.id)
            }
            val created = TurnRules.materialize(plan, slots, now, zone)
            if (created.isNotEmpty()) {
                dao.insertSlots(created)
                slots = dao.slotsFor(plan.id)
            }
            val changed = TurnRules.step(plan, slots, now, settings.missedTimeoutMin, zone)
            if (changed.isNotEmpty()) {
                dao.updateSlots(changed)
                changed.filter { it.status == TurnSlotStatus.MISSED }.forEach { scheduler.cancelTurn(it.id) }
                slots = dao.slotsFor(plan.id)
            }
            if (TurnRules.shouldAutoStop(plan, slots, now)) dao.updatePlan(plan.copy(status = TurnPlanStatus.STOPPED))
        }
    }

    suspend fun arm(now: Long) {
        val zone = zone()
        for (plan in dao.allPlans()) {
            TurnRules.arm(plan, dao.slotsFor(plan.id), now, settings.missedTimeoutMin, zone).requests.forEach { scheduler.set(it) }
        }
    }

    suspend fun audible(now: Long): List<TurnSlot> = dao.ringingSlots().filter { TurnRules.isAudible(it, now) }

    suspend fun overdueBeds(now: Long): List<String> {
        val overdue = dao.ringingSlots().filter { TurnRules.isOverdue(it, now, settings.missedTimeoutMin) }.map { it.planId }.toSet()
        return dao.allPlans().filter { it.id in overdue }.map { it.bedLabel }.sorted()
    }

    suspend fun snooze(audible: List<TurnSlot>, until: Long) {
        dao.updateSlots(audible.map { it.copy(snoozeCount = it.snoozeCount + 1, snoozeUntil = until) })
    }

    /** "Start round": the sound stops, the checklist shows; the slots stay open until ticked or missed. */
    suspend fun startRound() {
        dao.updateSlots(dao.ringingSlots().map { it.copy(silenced = true, snoozeUntil = null) })
    }

    /** ✓ — turned to [positionTo]: logged, and the bed's current position moves on. */
    suspend fun turned(slotId: Long, positionTo: app.transfusionvs.data.Position, now: Long) {
        val s = dao.slots(listOf(slotId)).firstOrNull()?.takeIf { it.status == TurnSlotStatus.RINGING } ?: return
        dao.updateSlots(listOf(s.copy(status = TurnSlotStatus.ACKED, ackAt = now, positionTo = positionTo, snoozeUntil = null, remindAt = null)))
        dao.plan(s.planId)?.let { dao.updatePlan(it.copy(currentPosition = positionTo)) }
        scheduler.cancelTurn(slotId)
    }

    /** "Can't turn" with a reason: logged as SKIPPED, not MISSED. */
    suspend fun cantTurn(slotId: Long, reason: app.transfusionvs.data.SkipReason, now: Long) {
        val s = dao.slots(listOf(slotId)).firstOrNull()?.takeIf { it.status == TurnSlotStatus.RINGING } ?: return
        dao.updateSlots(listOf(s.copy(status = TurnSlotStatus.SKIPPED, ackAt = now, skipReason = reason, snoozeUntil = null, remindAt = null)))
        scheduler.cancelTurn(slotId)
    }

    /** "Remind me in 30 min" (the round screen only offers it when it lands before the next slot). */
    suspend fun remind(until: Long) {
        dao.updateSlots(dao.ringingSlots().filter { it.silenced }.map { it.copy(remindAt = until) })
    }

    /** Stop: nothing more rings for this bed; open and upcoming slots are cancelled. */
    suspend fun stop(planId: Long, now: Long) {
        val plan = dao.plan(planId) ?: return
        dao.updatePlan(plan.copy(status = TurnPlanStatus.STOPPED, stopAt = now))
        cancelOpen(planId, includeRinging = true)
    }

    /** Edits reschedule what is still pending (done slots are history). */
    suspend fun cancelOpen(planId: Long, includeRinging: Boolean) {
        val open = dao.slotsFor(planId).filter {
            it.status == TurnSlotStatus.PENDING || (includeRinging && it.status == TurnSlotStatus.RINGING)
        }
        open.forEach { scheduler.cancelTurn(it.id) }
        val pending = open.filter { it.status == TurnSlotStatus.PENDING }.map { it.id }
        if (pending.isNotEmpty()) dao.deleteSlots(pending)
        val ringing = open.filter { it.status == TurnSlotStatus.RINGING }
        if (ringing.isNotEmpty()) dao.updateSlots(ringing.map { it.copy(status = TurnSlotStatus.CANCELLED) })
    }
}

package app.transfusionvs.alarm

import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.Settings
import app.transfusionvs.data.WardDao
import app.transfusionvs.schedule.CustomRules

/**
 * Custom timers as a third source for the shared alarm engine, in the shape of [TurnSource]: it
 * keeps its alarms up to date and tells the engine which system alarms to set. Ringing, snooze,
 * ring-now, notifications and re-arming stay in [Engine] and [AlarmService].
 * Called only while the engine holds its lock.
 */
class CustomSource(
    private val dao: WardDao,
    private val settings: Settings,
    private val scheduler: AlarmScheduler,
) {
    /** Create the next alarms, ring what is due, silence or close what timed out, auto-stop at the stop time. */
    suspend fun refresh(now: Long) {
        // Stopped timers go a day later, and with them the MRN and the name.
        dao.deleteStoppedCustomBefore(now - Engine.RETENTION_MS)
        dao.deleteOldCustomSlots(now - CustomRules.KEEP_MS)
        for (timer in dao.allCustomTimers()) {
            var slots = dao.customSlotsFor(timer.id)
            val created = CustomRules.materialize(timer, slots, now)
            if (created.isNotEmpty()) {
                dao.insertCustomSlots(created)
                slots = dao.customSlotsFor(timer.id)
            }
            val changed = CustomRules.step(timer, slots, now, settings.missedTimeoutMin)
            if (changed.isNotEmpty()) {
                dao.updateCustomSlots(changed)
                changed.filter { it.status == CustomSlotStatus.MISSED }.forEach { scheduler.cancelCustom(it.id) }
                slots = dao.customSlotsFor(timer.id)
            }
            if (CustomRules.shouldAutoStop(timer, slots, now)) {
                dao.updateCustomTimer(timer.copy(status = CustomTimerStatus.STOPPED, stoppedAt = now))
            }
        }
    }

    suspend fun arm(now: Long) {
        for (timer in dao.allCustomTimers()) {
            CustomRules.arm(timer, dao.customSlotsFor(timer.id), now, settings.missedTimeoutMin, settings.preAlertMin)
                .requests.forEach { scheduler.set(it) }
        }
    }

    suspend fun audible(now: Long): List<CustomSlot> = dao.ringingCustomSlots().filter { CustomRules.isAudible(it, now) }

    /** Every sounding custom alarm can be snoozed (none would pass its next alarm). */
    suspend fun canSnooze(audible: List<CustomSlot>, now: Long): Boolean = audible.all { s ->
        val timer = dao.customTimer(s.timerId) ?: return@all true
        CustomRules.canSnooze(timer, s, now, settings.snoozeMin)
    }

    suspend fun overdue(now: Long): List<Pair<CustomTimer, CustomSlot>> =
        dao.ringingCustomSlots().filter { CustomRules.isOverdue(it, now, settings.missedTimeoutMin) }
            .mapNotNull { s -> dao.customTimer(s.timerId)?.let { it to s } }

    suspend fun snooze(audible: List<CustomSlot>, until: Long) {
        dao.updateCustomSlots(audible.map { it.copy(snoozeCount = it.snoozeCount + 1, snoozeUntil = until) })
    }

    /** "Done ✓": open alarms (ringing, snoozed or overdue) are done. */
    suspend fun acknowledge(slots: List<CustomSlot>, now: Long) {
        val open = slots.filter { it.status == CustomSlotStatus.RINGING }
        dao.updateCustomSlots(open.map { it.copy(status = CustomSlotStatus.ACKED, ackAt = now, snoozeUntil = null) })
        open.forEach { scheduler.cancelCustom(it.id) }
    }

    /** Stop: nothing more rings; open and upcoming alarms are cancelled. */
    suspend fun stop(timerId: Long, now: Long) {
        val timer = dao.customTimer(timerId) ?: return
        dao.updateCustomTimer(timer.copy(status = CustomTimerStatus.STOPPED, stoppedAt = now))
        cancelOpen(timerId, includeRinging = true)
    }

    /** Edits reschedule what is still pending (done alarms stay as they were). */
    suspend fun cancelOpen(timerId: Long, includeRinging: Boolean) {
        val open = dao.customSlotsFor(timerId).filter {
            it.status == CustomSlotStatus.PENDING || (includeRinging && it.status == CustomSlotStatus.RINGING)
        }
        open.forEach { scheduler.cancelCustom(it.id) }
        val pending = open.filter { it.status == CustomSlotStatus.PENDING }.map { it.id }
        if (pending.isNotEmpty()) dao.deleteCustomSlots(pending)
        val ringing = open.filter { it.status == CustomSlotStatus.RINGING }
        if (ringing.isNotEmpty()) dao.updateCustomSlots(ringing.map { it.copy(status = CustomSlotStatus.CANCELLED, snoozeUntil = null) })
    }
}

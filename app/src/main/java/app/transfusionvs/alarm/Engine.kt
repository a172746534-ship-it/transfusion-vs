package app.transfusionvs.alarm

import android.content.Context
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus.ACKED
import app.transfusionvs.data.CheckpointStatus.CANCELLED
import app.transfusionvs.data.CheckpointStatus.MISSED
import app.transfusionvs.data.CheckpointStatus.PENDING
import app.transfusionvs.data.CheckpointStatus.RINGING
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.Settings
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TurnDao
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.data.TvsDao
import app.transfusionvs.data.UndoAction
import app.transfusionvs.data.WardDao
import app.transfusionvs.schedule.ArmPlanner
import app.transfusionvs.schedule.Grouping
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.Reconciler
import app.transfusionvs.schedule.ScheduleCalculator
import app.transfusionvs.schedule.ScheduleConfig
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.ui.screens.TurnPlanDraft
import app.transfusionvs.widget.BedsWidget
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The alarm engine. The database is the single source of truth; every entry point
 * (alarm fired, button tapped, phone rebooted, time changed, app opened) refreshes state from
 * it and re-arms the system alarms. All of it is idempotent, so running it twice is harmless.
 *
 * Starting the ringing service is only allowed from contexts Android permits (an exact alarm
 * firing, a visible screen). Everywhere else the engine sets one immediate alarm instead,
 * which then rings everything due together.
 */
class Engine(
    private val context: Context,
    private val dao: TvsDao,
    private val turnDao: TurnDao,
    private val wardDao: WardDao,
    private val settings: Settings,
    private val scheduler: AlarmScheduler,
) {
    private val mutex = Mutex()

    /** Repositioning: a second source on the same engine (same scheduler, service and notifications). */
    private val turns = TurnSource(turnDao, settings, scheduler)

    private fun now() = System.currentTimeMillis()

    // ---- Entry points ----

    /** A checkpoint, snooze, missed-timer or ring-now alarm fired. */
    suspend fun onAlarm() = update(canStartService = true)

    /**
     * Boot, app update, time or timezone change, permission change, app opened.
     * @param canStartService true only when one of our screens is visible.
     */
    suspend fun rearm(canStartService: Boolean = false) = update(canStartService)

    suspend fun onPreAlert(checkpointId: Long) {
        val row = dao.withBed(listOf(checkpointId)).firstOrNull() ?: return
        if (row.status == PENDING && row.scheduledAt > now()) {
            Notifications.preAlert(context, row, settings.preAlertMin)
        }
    }

    /** "Measured & documented ✓". On a MISSED checkpoint this is the late acknowledge. */
    suspend fun acknowledge(ids: List<Long>) {
        if (ids.isEmpty()) return
        val now = now()
        var confirmed = false
        mutex.withLock {
            val before = dao.checkpoints(ids)
            val changed = before.mapNotNull { c ->
                when {
                    c.status == RINGING -> c.copy(status = ACKED, ackAt = now, snoozeUntil = null)
                    c.status == MISSED && c.ackAt == null -> c.copy(ackAt = now)
                    c.status == PENDING && c.type == CheckpointType.END -> c.copy(status = ACKED, ackAt = now)
                    else -> null
                }
            }
            confirmed = changed.isNotEmpty()
            recordAckUndoLocked(before, changed, now)
            dao.updateCheckpoints(changed)
            changed.forEach { scheduler.cancel(it.id) }
            closeFinishedTestsLocked(now)
        }
        if (confirmed) Haptics.confirmed(context)
        syncRinging(canStartService = false)
        syncOverdue()
    }

    /** One entry per bed, so "Undo last" on a card only touches that bed. Test alarms have nothing to undo. */
    private suspend fun recordAckUndoLocked(before: List<Checkpoint>, changed: List<Checkpoint>, now: Long) {
        for ((transfusionId, rows) in changed.groupBy { it.transfusionId }) {
            val t = dao.transfusion(transfusionId) ?: continue
            if (t.isTest) continue
            val ids = rows.map { it.id }.toSet()
            val what = rows.joinToString(" + ") { Labels.title(it.type, it.offsetMin) }
            recordUndoLocked(
                UndoAction.transfusion(transfusionId), "${t.bedLabel} · $what done", now,
                before.filter { it.id in ids }.map { UndoStep.of(it) },
            )
        }
    }

    private suspend fun recordUndoLocked(ownerKey: String, label: String, now: Long, steps: List<UndoStep>) {
        if (steps.isEmpty()) return
        wardDao.insertUndo(UndoAction(ownerKey = ownerKey, label = label, at = now, expiresAt = now + UNDO_MS, snapshot = UndoCodec.encode(steps)))
    }

    /** Puts back exactly what the action changed. A whole round goes back together. */
    suspend fun undo(id: Long) {
        val now = now()
        mutex.withLock {
            val action = wardDao.undo(id) ?: return@withLock
            if (action.expiresAt > now) {
                UndoCodec.decode(action.snapshot).forEach { applyUndoLocked(it) }
            }
            when (action.ownerKey) {
                // The per-bed entries of a round undone as a whole are spent too, and so is the whole
                // once one of its beds went back.
                UndoAction.ROUND -> wardDao.deleteAllUndoAt(action.at)
                else -> {
                    wardDao.deleteUndo(id)
                    wardDao.deleteUndoAt(action.at, UndoAction.ROUND)
                }
            }
        }
        update(canStartService = true)
    }

    private suspend fun applyUndoLocked(step: UndoStep) {
        when (step) {
            is UndoStep.RestoreCheckpoint -> dao.checkpoints(listOf(step.id)).firstOrNull()?.let {
                dao.updateCheckpoints(
                    listOf(it.copy(status = step.status, firedAt = step.firedAt, ackAt = step.ackAt, snoozeCount = step.snoozeCount, snoozeUntil = step.snoozeUntil)),
                )
            }
            is UndoStep.RestoreSlot -> turnDao.slots(listOf(step.id)).firstOrNull()?.let {
                turnDao.updateSlots(
                    listOf(
                        it.copy(
                            status = step.status, firedAt = step.firedAt, ackAt = step.ackAt, positionFrom = step.positionFrom,
                            positionTo = step.positionTo, skipReason = step.skipReason, snoozeCount = step.snoozeCount,
                            snoozeUntil = step.snoozeUntil, silenced = step.silenced, remindAt = step.remindAt,
                        ),
                    ),
                )
            }
            is UndoStep.RestoreTransfusion -> dao.transfusion(step.id)?.let { dao.updateTransfusion(it.copy(status = step.status, endAt = step.endAt)) }
            is UndoStep.RestorePlan -> turnDao.plan(step.id)?.let {
                turnDao.updatePlan(it.copy(status = step.status, stopAt = step.stopAt, currentPosition = step.current))
            }
            is UndoStep.DeleteCheckpoint -> {
                scheduler.cancel(step.id)
                dao.deleteCheckpoints(listOf(step.id))
            }
            is UndoStep.DeleteTransfusion -> {
                dao.checkpointsFor(step.id).forEach { scheduler.cancel(it.id) }
                dao.deleteTransfusion(step.id)
            }
            is UndoStep.DeletePlan -> {
                turns.cancelOpen(step.id, includeRinging = true)
                turnDao.deletePlan(step.id)
            }
        }
    }

    /** Snoozes everything that is sounding. Not offered once any of it reached the max snoozes. */
    suspend fun snooze() {
        val now = now()
        mutex.withLock {
            val audible = dao.activeCheckpoints().filter { Grouping.isAudible(it, now) }
            val audibleTurns = turns.audible(now)
            if (!canSnoozeCounts(audible.map { it.snoozeCount } + audibleTurns.map { it.snoozeCount })) return@withLock
            val until = now + settings.snoozeMin * MINUTE_MS
            dao.updateCheckpoints(audible.map { it.copy(snoozeCount = it.snoozeCount + 1, snoozeUntil = until) })
            turns.snooze(audibleTurns, until)
            armLocked(now)
        }
        syncRinging(canStartService = false)
    }

    // ---- Repositioning (called from visible screens or the alarm's own buttons) ----

    suspend fun startTurnPlan(draft: TurnPlanDraft) {
        val now = now()
        mutex.withLock {
            val used = (dao.runningColors() + turnDao.runningColors()).toSet()
            val color = (0 until BED_COLOR_COUNT).firstOrNull { it !in used } ?: (used.size % BED_COLOR_COUNT)
            val id = turnDao.insertPlan(
                TurnPlan(
                    bedLabel = draft.bedLabel.trim(), colorIndex = color, intervalMin = draft.intervalMin,
                    startAt = now, firstSlotAt = draft.firstSlotAt, stopAt = draft.stopAt,
                    rotation = Position.toCsv(draft.rotation), currentPosition = draft.current,
                ),
            )
            recordUndoLocked(UndoAction.plan(id), "${draft.bedLabel.trim()} · turning started", now, listOf(UndoStep.DeletePlan(id)))
        }
        update(canStartService = true)
    }

    /** Changing the positions, interval or stop time reschedules everything still pending. */
    suspend fun editTurnPlan(id: Long, draft: TurnPlanDraft) {
        mutex.withLock {
            val p = turnDao.plan(id)
            if (p != null && p.status == TurnPlanStatus.RUNNING) {
                turns.cancelOpen(id, includeRinging = false)
                turnDao.updatePlan(
                    p.copy(
                        bedLabel = draft.bedLabel.trim(), intervalMin = draft.intervalMin,
                        // The grid doesn't move on an edit, unless the interval itself changed.
                        firstSlotAt = if (draft.intervalMin == p.intervalMin) p.firstSlotAt else draft.firstSlotAt,
                        stopAt = draft.stopAt, rotation = Position.toCsv(draft.rotation), currentPosition = draft.current,
                    ),
                )
            }
        }
        update(canStartService = true)
    }

    suspend fun stopTurnPlan(id: Long) {
        val now = now()
        mutex.withLock {
            turnDao.plan(id)?.let { p ->
                val ringing = turnDao.slotsFor(id).filter { it.status == TurnSlotStatus.RINGING }
                recordUndoLocked(UndoAction.plan(id), "${p.bedLabel} · turning stopped", now, listOf(UndoStep.of(p)) + ringing.map { UndoStep.of(it) })
            }
            turns.stop(id, now)
        }
        update(canStartService = true)
    }

    /** Restart: the same bed and positions, from the next slot, with no stop time. */
    suspend fun restartTurnPlan(id: Long) {
        val now = now()
        mutex.withLock {
            turnDao.plan(id)?.let { p ->
                turnDao.updatePlan(
                    p.copy(
                        status = TurnPlanStatus.RUNNING, startAt = now, stopAt = null,
                        firstSlotAt = TurnGrid.firstSlot(now, p.intervalMin, ZoneId.systemDefault()),
                    ),
                )
            }
        }
        update(canStartService = true)
    }

    /** "Start round": stops the sound; the checklist shows. */
    suspend fun startRound() {
        val now = now()
        mutex.withLock {
            turns.startRound()
            armLocked(now)
        }
        syncRinging(canStartService = false)
        syncOverdue()
    }

    suspend fun turned(slotId: Long, position: Position) {
        val now = now()
        mutex.withLock {
            recordTurnUndoLocked(slotId, now) { "$it · turned to ${position.label}" }
            turns.turned(slotId, position, now)
        }
        Haptics.confirmed(context)
        syncRinging(canStartService = false)
        syncOverdue()
    }

    suspend fun cantTurn(slotId: Long, reason: SkipReason) {
        val now = now()
        mutex.withLock {
            recordTurnUndoLocked(slotId, now) { "$it · can't turn: ${reason.label}" }
            turns.cantTurn(slotId, reason, now)
        }
        Haptics.confirmed(context)
        syncRinging(canStartService = false)
        syncOverdue()
    }

    /** "All turned as suggested ✓": each open bed moves to its suggested position, in one tap. */
    suspend fun allTurned() {
        val now = now()
        mutex.withLock {
            val round = RoundBuilder.build(turnDao.round(), now, ZoneId.systemDefault()) ?: return@withLock
            val all = mutableListOf<UndoStep>()
            for (bed in round.beds) {
                val steps = turnStepsLocked(bed.slotId)
                all += steps
                recordUndoLocked(UndoAction.plan(bed.planId), "${bed.bedLabel} · turned to ${bed.suggested.label}", now, steps)
                turns.turned(bed.slotId, bed.suggested, now)
            }
            if (round.beds.size > 1) {
                recordUndoLocked(UndoAction.ROUND, "${round.beds.size} beds turned as suggested", now, all)
            }
        }
        Haptics.confirmed(context)
        syncRinging(canStartService = false)
        syncOverdue()
    }

    private suspend fun turnStepsLocked(slotId: Long): List<UndoStep> {
        val slot = turnDao.slots(listOf(slotId)).firstOrNull() ?: return emptyList()
        val plan = turnDao.plan(slot.planId) ?: return emptyList()
        return listOf(UndoStep.of(slot), UndoStep.of(plan))
    }

    private suspend fun recordTurnUndoLocked(slotId: Long, now: Long, label: (bed: String) -> String) {
        val slot = turnDao.slots(listOf(slotId)).firstOrNull()?.takeIf { it.status == TurnSlotStatus.RINGING } ?: return
        val plan = turnDao.plan(slot.planId) ?: return
        recordUndoLocked(UndoAction.plan(plan.id), label(plan.bedLabel), now, listOf(UndoStep.of(slot), UndoStep.of(plan)))
    }

    /** "Remind me in 30 min": offered only if it lands before the next slot. */
    suspend fun remindTurns() {
        val now = now()
        mutex.withLock {
            val round = RoundBuilder.build(turnDao.round(), now, ZoneId.systemDefault())
            if (round != null && round.canRemind) {
                turns.remind(now + TurnGrid.REMIND_MIN * MINUTE_MS)
                armLocked(now)
            }
        }
        syncOverdue()
    }

    /** Setup check → "Test alarm in …": a real alarm through the full path, excluded from stats. */
    suspend fun scheduleTest(delayMs: Long) {
        val now = now()
        mutex.withLock {
            val id = dao.insertTransfusion(
                Transfusion(
                    bedLabel = "Test", colorIndex = 0, startAt = now, plannedDurationMin = 0,
                    baselineConfirmed = true, isTest = true,
                ),
            )
            dao.insertCheckpoints(
                listOf(
                    Checkpoint(
                        transfusionId = id, type = CheckpointType.TEST,
                        offsetMin = (delayMs / MINUTE_MS).toInt(), scheduledAt = now + delayMs,
                    ),
                ),
            )
        }
        update(canStartService = true)
    }

    suspend fun cancelTest(checkpointId: Long) {
        val now = now()
        mutex.withLock {
            val rows = dao.checkpoints(listOf(checkpointId))
                .filter { it.type == CheckpointType.TEST && (it.status == PENDING || it.status == RINGING) }
            dao.updateCheckpoints(rows.map { it.copy(status = CANCELLED, snoozeUntil = null) })
            rows.forEach { scheduler.cancel(it.id) }
            closeFinishedTestsLocked(now)
        }
        syncRinging(canStartService = false)
    }

    // ---- Transfusions (called from visible screens, so the ringing service may be started) ----

    /** Start is only offered after "Baseline VS taken". T0 is never in the future. */
    suspend fun startTransfusion(bedLabel: String, startAt: Long, plannedDurationMin: Int): Long {
        val now = now()
        val id = mutex.withLock {
            val used = dao.runningColors().toSet()
            val color = (0 until BED_COLOR_COUNT).firstOrNull { it !in used } ?: (used.size % BED_COLOR_COUNT)
            dao.insertTransfusion(
                Transfusion(
                    bedLabel = bedLabel.trim(),
                    colorIndex = color,
                    startAt = ScheduleCalculator.truncateToMinute(minOf(startAt, now)),
                    plannedDurationMin = plannedDurationMin,
                    baselineConfirmed = true,
                ),
            ).also {
                recordUndoLocked(UndoAction.transfusion(it), "${bedLabel.trim()} · transfusion started", now, listOf(UndoStep.DeleteTransfusion(it)))
            }
        }
        update(canStartService = true) // creates the checkpoints; passed ones ring as one alarm
        return id
    }

    /** Editing the start time or duration reschedules everything still pending. */
    suspend fun editTransfusion(id: Long, bedLabel: String, startAt: Long, plannedDurationMin: Int) {
        val now = now()
        mutex.withLock {
            val t = dao.transfusion(id)
            if (t != null && t.status == TransfusionStatus.RUNNING) {
                dao.updateTransfusion(
                    t.copy(
                        bedLabel = bedLabel.trim(),
                        startAt = ScheduleCalculator.truncateToMinute(minOf(startAt, now)),
                        plannedDurationMin = plannedDurationMin,
                    ),
                )
            }
        }
        update(canStartService = true)
    }

    /** End: logs the end time, cancels what is still pending, and creates the End VS prompt. */
    suspend fun endTransfusion(id: Long) {
        val now = now()
        mutex.withLock {
            val t = dao.transfusion(id)
            if (t != null && t.status == TransfusionStatus.RUNNING) {
                val cancelled = dao.checkpointsFor(id).filter { it.status == PENDING }
                dao.updateTransfusion(t.copy(status = TransfusionStatus.ENDED, endAt = now))
                cancelOpenLocked(id, includeRinging = false)
                val endRow = dao.insertCheckpoints(
                    listOf(
                        Checkpoint(
                            transfusionId = id, type = CheckpointType.END,
                            offsetMin = ((now - t.startAt) / MINUTE_MS).toInt(), scheduledAt = now,
                        ),
                    ),
                )
                recordUndoLocked(
                    UndoAction.transfusion(id), "${t.bedLabel} · transfusion ended", now,
                    listOf(UndoStep.of(t)) + cancelled.map { UndoStep.of(it) } + endRow.map { UndoStep.DeleteCheckpoint(it) },
                )
            }
        }
        update(canStartService = true)
    }

    /** "Transfusion stopped (reaction)": cancels that bed's remaining alarms. No clinical details are stored. */
    suspend fun stopForReaction(id: Long) {
        val now = now()
        mutex.withLock {
            val t = dao.transfusion(id)
            if (t != null && t.status != TransfusionStatus.STOPPED_REACTION) {
                val open = dao.checkpointsFor(id).filter { it.status == PENDING || it.status == RINGING }
                dao.updateTransfusion(t.copy(status = TransfusionStatus.STOPPED_REACTION, endAt = t.endAt ?: now))
                cancelOpenLocked(id, includeRinging = true)
                recordUndoLocked(
                    UndoAction.transfusion(id), "${t.bedLabel} · marked stopped", now,
                    listOf(UndoStep.of(t)) + open.map { UndoStep.of(it) },
                )
            }
        }
        update(canStartService = true)
    }

    /** "Remove from Home" on a stopped bed. It would go by itself a day later. */
    suspend fun forgetStopped(id: Long) {
        mutex.withLock {
            val t = dao.transfusion(id)
            if (t != null && t.status == TransfusionStatus.STOPPED_REACTION) dao.deleteTransfusion(id)
        }
        update(canStartService = true)
    }

    private suspend fun cancelOpenLocked(transfusionId: Long, includeRinging: Boolean) {
        val open = dao.checkpointsFor(transfusionId).filter {
            it.status == PENDING || (includeRinging && it.status == RINGING)
        }
        dao.updateCheckpoints(open.map { it.copy(status = CANCELLED, snoozeUntil = null) })
        open.forEach { scheduler.cancel(it.id) }
    }

    /** Re-posts notifications the user swiped away (they come back until acknowledged). */
    suspend fun refreshNotifications() {
        syncRinging(canStartService = false)
        syncOverdue()
    }

    // ---- Core refresh ----

    private suspend fun update(canStartService: Boolean) {
        val now = now()
        mutex.withLock { refreshLocked(now) }
        syncRinging(canStartService)
        syncOverdue()
    }

    private suspend fun refreshLocked(now: Long) {
        // 0. Finished transfusions are deleted a day later (Cerner is the record, not the phone).
        dao.deleteFinishedBefore(now - RETENTION_MS)
        wardDao.deleteExpiredUndo(now)

        // 1. Missed timer: 10 min after the first ring (snoozes don't extend it).
        val timedOut = Grouping.timedOut(dao.activeCheckpoints(), now, settings.missedTimeoutMin)
        if (timedOut.isNotEmpty()) {
            dao.updateCheckpoints(timedOut.map { it.copy(status = MISSED, snoozeUntil = null) })
            timedOut.forEach { scheduler.cancel(it.id) }
        }

        // 2. Create upcoming checkpoints for running transfusions (rolling window).
        for (t in dao.runningTransfusions()) {
            if (!t.isTest) materializeLocked(t, now)
        }

        // 3. Everything due now (any bed) rings together; finished snoozes ring again.
        val active = dao.activeCheckpoints()
        val due = Grouping.dueNow(active, now)
        val snoozeOver = Grouping.snoozeOver(active, now)
        due.forEach { scheduler.cancel(it.id) }
        dao.updateCheckpoints(
            due.map { it.copy(status = RINGING, firedAt = now, snoozeUntil = null) } +
                snoozeOver.map { it.copy(snoozeUntil = null) },
        )

        // 4. Repositioning: same refresh, its own rules.
        turns.refresh(now)

        closeFinishedTestsLocked(now)
        armLocked(now)
    }

    private suspend fun materializeLocked(t: Transfusion, now: Long) {
        val config = ScheduleConfig(t.plannedDurationMin, settings.limitMin, settings.warnBeforeMin)
        val planned = ScheduleCalculator.plan(config, ScheduleCalculator.horizonOffsetMin(t.startAt, config, now))
        val result = Reconciler.reconcile(t.id, t.startAt, dao.checkpointsFor(t.id), planned, now)
        if (result.delete.isNotEmpty()) {
            result.delete.forEach { scheduler.cancel(it) }
            dao.deleteCheckpoints(result.delete)
        }
        if (result.update.isNotEmpty()) dao.updateCheckpoints(result.update)
        if (result.insert.isNotEmpty()) dao.insertCheckpoints(result.insert)
    }

    private suspend fun armLocked(now: Long) {
        val plan = ArmPlanner.plan(dao.activeCheckpoints(), now, settings.preAlertMin, settings.missedTimeoutMin)
        plan.requests.forEach { scheduler.set(it) }
        turns.arm(now)
    }

    private suspend fun closeFinishedTestsLocked(now: Long) {
        for (t in dao.runningTransfusions().filter { it.isTest }) {
            val open = dao.checkpointsFor(t.id).any { it.status == PENDING || it.status == RINGING }
            if (!open) dao.updateTransfusion(t.copy(status = TransfusionStatus.ENDED, endAt = now))
        }
    }

    // ---- Ringing and notifications ----

    /** Starts, updates or stops the ringing service so it matches what is sounding right now. */
    suspend fun syncRinging(canStartService: Boolean) {
        val now = now()
        val ringing = dao.ringingWithBed()
        val audible = ringing.filter { it.snoozeUntil == null || it.snoozeUntil <= now }
        val audibleTurns = turns.audible(now)
        if (audible.isEmpty() && audibleTurns.isEmpty()) {
            AlarmService.stop(context)
            return
        }
        // Sounding turns join the same alarm: only a round that is sounding counts as "round" here,
        // so a transfusion alone keeps its own sound while a started round is being ticked.
        val round = if (audibleTurns.isEmpty()) null else RoundBuilder.build(turnDao.round(), now, ZoneId.systemDefault())
        val spec = RingSpec.build(
            audible,
            canSnooze = canSnoozeCounts(audible.map { it.snoozeCount } + audibleTurns.map { it.snoozeCount }),
            snoozeMin = settings.snoozeMin,
            soundOn = settings.soundOn,
            soundUri = settings.alarmSoundUri,
            missedTimeoutMin = settings.missedTimeoutMin,
            round = round,
            turnSoundUri = settings.turnSoundUri,
        )
        when {
            AlarmService.isRunning -> AlarmService.update(spec)
            canStartService -> AlarmService.start(context, spec)
            else -> scheduler.setRingNow(now) // let an exact alarm start it, which Android always allows
        }
    }

    private suspend fun syncOverdue() {
        val missed = dao.missedUnacknowledged()
        val rows = if (missed.isEmpty()) emptyList() else dao.withBed(missed.map { it.id })
        Notifications.syncOverdue(context, rows)

        val now = now()
        val round = RoundBuilder.build(turnDao.round(), now, ZoneId.systemDefault())
        Notifications.syncTurnOverdue(context, turns.overdueBeds(now), canRemind = round?.canRemind == true)
        BedsWidget.refresh(context, dao, turnDao, settings)
    }

    private fun canSnoozeCounts(snoozeCounts: List<Int>) =
        snoozeCounts.isNotEmpty() && snoozeCounts.all { it < Settings.MAX_SNOOZES }

    companion object {
        /** Size of the bed colour palette (kept for the data; the UI tells beds apart by label). */
        const val BED_COLOR_COUNT = 8

        /** How long a finished transfusion stays on the phone. */
        const val RETENTION_MS = 24 * 60 * 60 * 1000L

        /** "Undo last" stays on the bed's card this long. */
        const val UNDO_MS = 5 * 60 * 1000L
    }
}

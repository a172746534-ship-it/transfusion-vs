package app.transfusionvs.alarm

import android.content.Context
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus.ACKED
import app.transfusionvs.data.CheckpointStatus.CANCELLED
import app.transfusionvs.data.CheckpointStatus.MISSED
import app.transfusionvs.data.CheckpointStatus.PENDING
import app.transfusionvs.data.CheckpointStatus.RINGING
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Settings
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TvsDao
import app.transfusionvs.schedule.ArmPlanner
import app.transfusionvs.schedule.Grouping
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.Reconciler
import app.transfusionvs.schedule.ScheduleCalculator
import app.transfusionvs.schedule.ScheduleConfig
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
    private val settings: Settings,
    private val scheduler: AlarmScheduler,
) {
    private val mutex = Mutex()

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
        mutex.withLock {
            val changed = dao.checkpoints(ids).mapNotNull { c ->
                when {
                    c.status == RINGING -> c.copy(status = ACKED, ackAt = now, snoozeUntil = null)
                    c.status == MISSED && c.ackAt == null -> c.copy(ackAt = now)
                    c.status == PENDING && c.type == CheckpointType.END -> c.copy(status = ACKED, ackAt = now)
                    else -> null
                }
            }
            dao.updateCheckpoints(changed)
            changed.forEach { scheduler.cancel(it.id) }
            closeFinishedTestsLocked(now)
        }
        syncRinging(canStartService = false)
        syncOverdue()
    }

    /** Snoozes everything that is sounding. Not offered once any of it reached the max snoozes. */
    suspend fun snooze() {
        val now = now()
        mutex.withLock {
            val audible = dao.activeCheckpoints().filter { Grouping.isAudible(it, now) }
            if (!canSnooze(audible)) return@withLock
            val until = now + settings.snoozeMin * MINUTE_MS
            dao.updateCheckpoints(audible.map { it.copy(snoozeCount = it.snoozeCount + 1, snoozeUntil = until) })
            armLocked(now)
        }
        syncRinging(canStartService = false)
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
            )
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
                dao.updateTransfusion(t.copy(status = TransfusionStatus.ENDED, endAt = now))
                cancelOpenLocked(id, includeRinging = false)
                dao.insertCheckpoints(
                    listOf(
                        Checkpoint(
                            transfusionId = id, type = CheckpointType.END,
                            offsetMin = ((now - t.startAt) / MINUTE_MS).toInt(), scheduledAt = now,
                        ),
                    ),
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
                dao.updateTransfusion(t.copy(status = TransfusionStatus.STOPPED_REACTION, endAt = t.endAt ?: now))
                cancelOpenLocked(id, includeRinging = true)
            }
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

    /** "Delete all data": cancels every alarm, stops any ringing, and erases all transfusions and the log. */
    suspend fun deleteAllData() {
        mutex.withLock {
            dao.activeCheckpoints().forEach { scheduler.cancel(it.id) }
            scheduler.cancelRingNow()
            dao.deleteAllTransfusions()
        }
        AlarmService.stop(context)
        Notifications.cancelAll(context)
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
        if (audible.isEmpty()) {
            AlarmService.stop(context)
            return
        }
        val spec = RingSpec.build(
            audible,
            canSnooze = canSnoozeCounts(audible.map { it.snoozeCount }),
            snoozeMin = settings.snoozeMin,
            soundOn = settings.soundOn,
            soundUri = settings.alarmSoundUri,
            missedTimeoutMin = settings.missedTimeoutMin,
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
    }

    private fun canSnooze(audible: List<Checkpoint>) = canSnoozeCounts(audible.map { it.snoozeCount })

    private fun canSnoozeCounts(snoozeCounts: List<Int>) =
        snoozeCounts.isNotEmpty() && snoozeCounts.all { it < Settings.MAX_SNOOZES }

    companion object {
        /** Size of the bed colour palette (ui/theme/Theme.kt → Tvs.BedColors). */
        const val BED_COLOR_COUNT = 8
    }
}

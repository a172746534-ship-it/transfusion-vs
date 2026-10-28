package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType

// Pure decisions about which system alarms to set. AlarmScheduler turns them into AlarmManager calls.

enum class AlarmKind {
    /** The checkpoint itself (setAlarmClock). */
    CHECKPOINT,
    /** Re-ring after a snooze (setAlarmClock). */
    SNOOZE,
    /** Missed timer backup, survives process death (exact, allow-while-idle). */
    TIMEOUT,
    /** Optional gentle pre-alert (exact, allow-while-idle). */
    PREALERT,
}

data class AlarmRequest(val kind: AlarmKind, val checkpointId: Long, val at: Long)

/**
 * @param ringNow something is overdue or should be sounding right now; the engine answers with
 *   ONE immediate alarm that rings everything together.
 */
data class ArmPlan(val requests: List<AlarmRequest>, val ringNow: Boolean)

object ArmPlanner {

    fun plan(active: List<Checkpoint>, now: Long, preAlertMin: Int, missedTimeoutMin: Int): ArmPlan {
        val requests = mutableListOf<AlarmRequest>()
        var ringNow = false
        for (c in active) {
            when (c.status) {
                CheckpointStatus.PENDING -> {
                    if (!c.type.rings) continue
                    if (c.scheduledAt > now) {
                        requests += AlarmRequest(AlarmKind.CHECKPOINT, c.id, c.scheduledAt)
                        val pre = c.scheduledAt - preAlertMin * MINUTE_MS
                        if (preAlertMin > 0 && c.type != CheckpointType.TEST && pre > now) {
                            requests += AlarmRequest(AlarmKind.PREALERT, c.id, pre)
                        }
                    } else {
                        ringNow = true
                    }
                }
                CheckpointStatus.RINGING -> {
                    requests += AlarmRequest(AlarmKind.TIMEOUT, c.id, missedDeadline(c, now, missedTimeoutMin))
                    val snoozeUntil = c.snoozeUntil
                    if (snoozeUntil != null && snoozeUntil > now) {
                        requests += AlarmRequest(AlarmKind.SNOOZE, c.id, snoozeUntil)
                    } else {
                        ringNow = true
                    }
                }
                else -> Unit
            }
        }
        return ArmPlan(requests, ringNow)
    }

    /** The missed timer counts from the first ring; snoozes don't extend it. */
    fun missedDeadline(c: Checkpoint, now: Long, missedTimeoutMin: Int): Long =
        (c.firedAt ?: now) + missedTimeoutMin * MINUTE_MS
}

object Grouping {

    /**
     * PENDING checkpoints that are due now (any bed). Real checkpoints sit on whole minutes, so
     * everything in the same minute is promoted together and shown on one alarm screen.
     */
    fun dueNow(active: List<Checkpoint>, now: Long): List<Checkpoint> = active.filter {
        it.status == CheckpointStatus.PENDING && it.type.rings && it.scheduledAt <= now + 1_000
    }

    fun snoozeOver(active: List<Checkpoint>, now: Long): List<Checkpoint> = active.filter {
        it.status == CheckpointStatus.RINGING && it.snoozeUntil != null && it.snoozeUntil <= now
    }

    fun timedOut(active: List<Checkpoint>, now: Long, missedTimeoutMin: Int): List<Checkpoint> = active.filter {
        it.status == CheckpointStatus.RINGING && ArmPlanner.missedDeadline(it, now, missedTimeoutMin) <= now
    }

    /** RINGING and not snoozed. */
    fun isAudible(c: Checkpoint, now: Long): Boolean =
        c.status == CheckpointStatus.RINGING && (c.snoozeUntil == null || c.snoozeUntil <= now)
}

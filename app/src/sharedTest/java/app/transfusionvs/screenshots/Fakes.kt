package app.transfusionvs.screenshots

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.Reconciler
import app.transfusionvs.schedule.ScheduleCalculator
import app.transfusionvs.schedule.ScheduleConfig
import java.time.LocalDateTime
import java.time.ZoneId

/** Fixed, realistic data for screenshots. Everything happens on 15 Jan 2026, Cairo time. */
object Fakes {
    val zone: ZoneId = ZoneId.of("Africa/Cairo")
    fun at(local: String): Long = LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()

    /** "Now" in every screenshot. */
    val now = at("2026-01-15T11:05")

    private var nextId = 100L

    fun transfusion(
        id: Long,
        bed: String,
        color: Int,
        start: String,
        durationMin: Int,
        status: TransfusionStatus = TransfusionStatus.RUNNING,
    ): TransfusionWithCheckpoints {
        val t = Transfusion(
            id = id, bedLabel = bed, colorIndex = color, startAt = at(start), plannedDurationMin = durationMin,
            status = status, baselineConfirmed = true,
        )
        val config = ScheduleConfig(durationMin)
        val plan = ScheduleCalculator.plan(config, ScheduleCalculator.horizonOffsetMin(t.startAt, config, now))
        val rows = Reconciler.reconcile(id, t.startAt, emptyList(), plan, t.startAt).insert.map { it.copy(id = nextId++) }
        return TransfusionWithCheckpoints(t, rows)
    }

    /** Marks a checkpoint as done [ackAfterMin] minutes after it was due, or as missed when null. */
    fun TransfusionWithCheckpoints.with(type: CheckpointType, offset: Int, ackAfterMin: Int?): TransfusionWithCheckpoints =
        copy(
            checkpoints = checkpoints.map { c ->
                if (c.type != type || c.offsetMin != offset) {
                    c
                } else if (ackAfterMin == null) {
                    c.copy(status = CheckpointStatus.MISSED, firedAt = c.scheduledAt)
                } else {
                    c.copy(status = CheckpointStatus.ACKED, firedAt = c.scheduledAt, ackAt = c.scheduledAt + ackAfterMin * MINUTE_MS)
                }
            },
        )

    /** 7654812: 15-min check on time, 30-min check missed, next check 11:20. */
    val bed12 = transfusion(1, "7654812", 0, "2026-01-15T10:20", 120)
        .with(CheckpointType.M15, 15, ackAfterMin = 2)
        .with(CheckpointType.M30, 30, ackAfterMin = null)

    /** 7654814: started 10:52, nothing due yet. */
    val bed14 = transfusion(2, "7654814", 1, "2026-01-15T10:52", 60)

    val home = listOf(bed12, bed14)

    /** The ward's beds, entered once in Settings. */

    /** Every urgency at once: overdue, due now, soon, calm. */
    fun sixBeds(): List<TransfusionWithCheckpoints> = listOf(
        bed12,
        transfusion(3, "7654814", 1, "2026-01-15T10:50", 120).let { b ->
            b.copy(checkpoints = b.checkpoints.map { if (it.type == CheckpointType.M15) it.copy(status = CheckpointStatus.RINGING, firedAt = now) else it })
        },
        transfusion(4, "7654807", 2, "2026-01-15T10:15", 120).with(CheckpointType.M15, 15, 1).with(CheckpointType.M30, 30, 1),
        transfusion(5, "7654821", 3, "2026-01-15T10:52", 120),
        transfusion(6, "7654803", 4, "2026-01-15T10:46", 180).let { b ->
            b.copy(checkpoints = b.checkpoints.map { if (it.type == CheckpointType.M15) it.copy(status = CheckpointStatus.RINGING, firedAt = it.scheduledAt) else it })
        },
        transfusion(7, "7654800123", 5, "2026-01-15T10:00", 240).with(CheckpointType.M15, 15, 1).with(CheckpointType.M30, 30, 1).with(CheckpointType.HOURLY, 60, 2),
    )

    fun ringing(bed: TransfusionWithCheckpoints, type: CheckpointType, offset: Int, firedAt: Long = now): CheckpointWithBed {
        val c: Checkpoint = bed.checkpoints.first { it.type == type && it.offsetMin == offset }
        val t = bed.transfusion
        return CheckpointWithBed(
            id = c.id, transfusionId = t.id, type = c.type, offsetMin = c.offsetMin, scheduledAt = c.scheduledAt,
            firedAt = firedAt, ackAt = null, snoozeCount = 0, snoozeUntil = null, status = CheckpointStatus.RINGING,
            bedLabel = t.bedLabel, colorIndex = t.colorIndex, startAt = t.startAt,
            plannedDurationMin = t.plannedDurationMin, isTest = false,
        )
    }

    fun test(id: Long, scheduled: String, status: CheckpointStatus, fired: String? = null, ack: String? = null) = CheckpointWithBed(
        id = id, transfusionId = 900 + id, type = CheckpointType.TEST, offsetMin = 1, scheduledAt = at(scheduled),
        firedAt = fired?.let(::at), ackAt = ack?.let(::at), snoozeCount = 0, snoozeUntil = null, status = status,
        bedLabel = "Test", colorIndex = 0, startAt = at(scheduled) - MINUTE_MS, plannedDurationMin = 0, isTest = true,
    )
}

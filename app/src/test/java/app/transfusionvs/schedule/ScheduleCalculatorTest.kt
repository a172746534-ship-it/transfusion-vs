package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus.ACKED
import app.transfusionvs.data.CheckpointStatus.PENDING
import app.transfusionvs.data.CheckpointStatus.RINGING
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointType.HOURLY
import app.transfusionvs.data.CheckpointType.LIMIT_WARNING
import app.transfusionvs.data.CheckpointType.M15
import app.transfusionvs.data.CheckpointType.M30
import app.transfusionvs.data.CheckpointType.PLANNED_END
import app.transfusionvs.data.CheckpointType.START
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleCalculatorTest {

    private val cairo = ZoneId.of("Africa/Cairo")

    private fun at(local: String, zone: ZoneId = cairo): Long =
        LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()

    private fun hm(ms: Long, zone: ZoneId = cairo) = Labels.clock(ms, zone)

    private fun times(t0: Long, plan: List<PlannedCheckpoint>, zone: ZoneId = cairo) =
        plan.map { hm(ScheduleCalculator.timeOf(t0, it.offsetMin), zone) }

    /** Materialise the plan the way the engine does, for a fresh transfusion. */
    private fun rows(t0: Long, config: ScheduleConfig, now: Long, transfusionId: Long = 1, idBase: Long = 0): List<Checkpoint> {
        val plan = ScheduleCalculator.plan(config, ScheduleCalculator.horizonOffsetMin(t0, config, now))
        return Reconciler.reconcile(transfusionId, t0, emptyList(), plan, now).insert
            .mapIndexed { i, c -> c.copy(id = idBase + i + 1) }
    }

    // ---- The card's example ----

    @Test
    fun example1020_matchesTheCard() {
        val t0 = at("2026-01-15T10:20")
        val plan = ScheduleCalculator.plan(ScheduleConfig(plannedDurationMin = 240), untilOffsetMin = 240)

        assertEquals(PlannedCheckpoint(START, 0), plan.first())
        val alarms = plan.filter { it.type != START }
        assertEquals(listOf(M15, M30, HOURLY, HOURLY, HOURLY), alarms.take(5).map { it.type })
        assertEquals(listOf("10:35", "10:50", "11:20", "12:20", "13:20"), times(t0, alarms.take(5)))
    }

    @Test
    fun startIsLoggedWithoutAnAlarm() {
        val now = at("2026-01-15T10:20")
        val start = rows(now, ScheduleConfig(120), now).first { it.type == START }
        assertEquals(ACKED, start.status)
        val plan = ArmPlanner.plan(listOf(start), now, preAlertMin = 0, missedTimeoutMin = 10)
        assertTrue(plan.requests.isEmpty())
        assertFalse(plan.ringNow)
    }

    @Test
    fun t0IsTruncatedToTheWholeMinute() {
        assertEquals(at("2026-01-15T10:20"), ScheduleCalculator.truncateToMinute(at("2026-01-15T10:20:47.500")))
    }

    // ---- Planned end, limit, running past the end ----

    @Test
    fun plannedEndReplacesTheHourlyCheckOnTheSameMinute() {
        val plan = ScheduleCalculator.plan(ScheduleConfig(120), 240)
        assertTrue(PlannedCheckpoint(PLANNED_END, 120) in plan)
        assertFalse(PlannedCheckpoint(HOURLY, 120) in plan)
        assertEquals(1, plan.count { it.offsetMin == 120 })
    }

    @Test
    fun customDurationGetsItsOwnEndCheck() {
        val plan = ScheduleCalculator.plan(ScheduleConfig(90), 240)
        assertTrue(PlannedCheckpoint(HOURLY, 60) in plan)
        assertTrue(PlannedCheckpoint(PLANNED_END, 90) in plan)
        assertTrue(PlannedCheckpoint(HOURLY, 120) in plan)
    }

    @Test
    fun limitWarningDefaultsTo15MinBefore4h_andIsConfigurable() {
        assertTrue(PlannedCheckpoint(LIMIT_WARNING, 225) in ScheduleCalculator.plan(ScheduleConfig(120), 240))
        val custom = ScheduleCalculator.plan(ScheduleConfig(120, limitMin = 180, warnBeforeMin = 30), 240)
        assertTrue(PlannedCheckpoint(LIMIT_WARNING, 150) in custom)
    }

    @Test
    fun runningPastThePlannedEnd_hourlyChecksContinueAtTheSameMinute() {
        val t0 = at("2026-01-15T10:20")
        val config = ScheduleConfig(plannedDurationMin = 60)
        val now = at("2026-01-15T15:00") // 4 h 40 min in, still not ended
        val plan = ScheduleCalculator.plan(config, ScheduleCalculator.horizonOffsetMin(t0, config, now))

        assertTrue(PlannedCheckpoint(PLANNED_END, 60) in plan)
        val hourly = plan.filter { it.type == HOURLY }
        assertEquals(
            listOf("12:20", "13:20", "14:20", "15:20", "16:20", "17:20"),
            times(t0, hourly),
        )
        // Something is always scheduled after "now".
        assertTrue(hourly.any { ScheduleCalculator.timeOf(t0, it.offsetMin) > now })
    }

    // ---- Same minute across beds ----

    @Test
    fun checkpointsInTheSameMinuteAcrossBedsRingTogether() {
        val bed12 = Checkpoint(id = 1, transfusionId = 12, type = M15, offsetMin = 15, scheduledAt = at("2026-01-15T10:35"))
        val bed14 = Checkpoint(id = 2, transfusionId = 14, type = M30, offsetMin = 30, scheduledAt = at("2026-01-15T10:35"))
        val bed16 = Checkpoint(id = 3, transfusionId = 16, type = M15, offsetMin = 15, scheduledAt = at("2026-01-15T10:36"))
        val firedAt = at("2026-01-15T10:35:00.300")

        val due = Grouping.dueNow(listOf(bed12, bed14, bed16), firedAt)
        assertEquals(setOf(1L, 2L), due.map { it.id }.toSet())
    }

    // ---- Time and timezone changes ----

    @Test
    fun timezoneChangeKeepsTheSameInstants_onlyTheDisplayedClockChanges() {
        val t0 = at("2026-01-15T10:20") // Cairo, UTC+2 in January
        val plan = ScheduleCalculator.plan(ScheduleConfig(120), 120).filter { it.type != START }
        val instants = plan.map { ScheduleCalculator.timeOf(t0, it.offsetMin) }

        // Same plan, same T0 → identical instants no matter which zone the phone is in.
        assertEquals(instants, plan.map { ScheduleCalculator.timeOf(t0, it.offsetMin) })
        assertEquals(listOf("10:35", "10:50", "11:20", "12:20"), instants.map { hm(it, cairo) })
        assertEquals(listOf("12:35", "12:50", "13:20", "14:20"), instants.map { hm(it, ZoneId.of("Asia/Dubai")) })
        assertEquals(listOf("14:05", "14:20", "14:50", "15:50"), instants.map { hm(it, ZoneId.of("Asia/Kolkata")) })
    }

    @Test
    fun daylightSavingShiftKeepsHourlyChecksOnTheSameMinute_60RealMinutesApart() {
        // Egypt starts DST on the last Friday of April: 2026-04-24 00:00 → 01:00.
        val t0 = at("2026-04-23T22:20")
        val plan = ScheduleCalculator.plan(ScheduleConfig(240), 240).filter { it.type == HOURLY }
        val instants = plan.map { ScheduleCalculator.timeOf(t0, it.offsetMin) }

        assertEquals(listOf("23:20", "01:20", "02:20"), instants.map { hm(it) })
        instants.zipWithNext { a, b -> assertEquals(60 * MINUTE_MS, b - a) }
    }

    @Test
    fun clockJumpsForward_passedCheckpointsRingAsOneImmediateAlarm() {
        val t0 = at("2026-01-15T10:20")
        val all = rows(t0, ScheduleConfig(120), now = t0)
        val now = at("2026-01-15T11:00") // phone time changed

        val due = Grouping.dueNow(all, now)
        assertEquals(listOf(M15, M30), due.map { it.type })

        val plan = ArmPlanner.plan(all, now, preAlertMin = 0, missedTimeoutMin = 10)
        assertTrue(plan.ringNow)
        val scheduled = plan.requests.filter { it.kind == AlarmKind.CHECKPOINT }.map { hm(it.at) }
        assertEquals("11:20", scheduled.first())
        assertFalse(scheduled.contains("10:35"))
    }

    @Test
    fun pastStartTime_passedCheckpointsAreOverdue_futureOnesScheduled() {
        val now = at("2026-01-15T11:00")
        val t0 = at("2026-01-15T10:20") // "started 40 min ago"
        val all = rows(t0, ScheduleConfig(120), now)

        assertEquals(listOf(M15, M30), Grouping.dueNow(all, now).map { it.type })
        val plan = ArmPlanner.plan(all, now, preAlertMin = 0, missedTimeoutMin = 10)
        assertTrue(plan.ringNow)
        assertTrue(plan.requests.any { it.kind == AlarmKind.CHECKPOINT && hm(it.at) == "11:20" })
    }

    // ---- Edits ----

    @Test
    fun editingStartTime_keepsDoneRows_andMovesPendingOnes() {
        val t0 = at("2026-01-15T10:20")
        val existing = rows(t0, ScheduleConfig(120), now = t0).map {
            if (it.type == M15) it.copy(status = ACKED, ackAt = at("2026-01-15T10:36")) else it
        }
        val newT0 = at("2026-01-15T10:25")
        val config = ScheduleConfig(120)
        val plan = ScheduleCalculator.plan(config, ScheduleCalculator.horizonOffsetMin(newT0, config, newT0))
        val result = Reconciler.reconcile(1, newT0, existing, plan, now = at("2026-01-15T10:40"))

        assertTrue(result.update.none { it.type == M15 })
        val moved = result.update.associateBy { it.type to it.offsetMin }
        assertEquals("10:55", hm(moved.getValue(M30 to 30).scheduledAt))
        assertEquals("11:25", hm(moved.getValue(HOURLY to 60).scheduledAt))
        assertEquals("10:25", hm(moved.getValue(START to 0).scheduledAt))
        assertTrue(result.insert.isEmpty())
    }

    @Test
    fun editingDuration_movesTheEndCheck_withoutDoubleAlarms() {
        val t0 = at("2026-01-15T10:20")
        val existing = rows(t0, ScheduleConfig(120), now = t0)
        val config = ScheduleConfig(180)
        val plan = ScheduleCalculator.plan(config, ScheduleCalculator.horizonOffsetMin(t0, config, t0))
        val result = Reconciler.reconcile(1, t0, existing, plan, now = t0)

        val changed = result.update.associateBy { it.offsetMin }
        assertEquals(HOURLY, changed.getValue(120).type)
        assertEquals(PLANNED_END, changed.getValue(180).type)
        assertTrue(result.insert.none { it.offsetMin == 120 || it.offsetMin == 180 })
    }

    // ---- Missed timer ----

    @Test
    fun missedTimerCountsFromTheFirstRing_snoozeDoesNotExtendIt() {
        val ringing = Checkpoint(
            id = 1, transfusionId = 1, type = CheckpointType.M15, offsetMin = 15,
            scheduledAt = at("2026-01-15T10:35"), firedAt = at("2026-01-15T10:35"),
            snoozeCount = 3, snoozeUntil = at("2026-01-15T10:46"), status = RINGING,
        )
        assertTrue(Grouping.timedOut(listOf(ringing), at("2026-01-15T10:44:59"), 10).isEmpty())
        assertEquals(1, Grouping.timedOut(listOf(ringing), at("2026-01-15T10:45"), 10).size)
    }

    @Test
    fun snoozedCheckpointIsSilentUntilSnoozeEnds() {
        val snoozed = Checkpoint(
            id = 1, transfusionId = 1, type = CheckpointType.M15, offsetMin = 15,
            scheduledAt = at("2026-01-15T10:35"), firedAt = at("2026-01-15T10:35"),
            snoozeCount = 1, snoozeUntil = at("2026-01-15T10:37"), status = RINGING,
        )
        assertFalse(Grouping.isAudible(snoozed, at("2026-01-15T10:36")))
        val plan = ArmPlanner.plan(listOf(snoozed), at("2026-01-15T10:36"), 0, 10)
        assertFalse(plan.ringNow)
        assertTrue(plan.requests.any { it.kind == AlarmKind.SNOOZE && hm(it.at) == "10:37" })
        assertTrue(Grouping.isAudible(snoozed, at("2026-01-15T10:37")))
    }

    @Test
    fun preAlertIsScheduledBeforeEachFutureCheckpoint() {
        val t0 = at("2026-01-15T10:20")
        val all = rows(t0, ScheduleConfig(120), now = t0)
        val plan = ArmPlanner.plan(all, t0, preAlertMin = 2, missedTimeoutMin = 10)
        val pre = plan.requests.filter { it.kind == AlarmKind.PREALERT }.map { hm(it.at) }
        assertEquals(listOf("10:33", "10:48", "11:18"), pre.take(3))
        assertTrue(all.none { it.status == PENDING && it.type == START })
    }
}

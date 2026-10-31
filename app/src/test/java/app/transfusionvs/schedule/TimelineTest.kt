package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Transfusion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimelineTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private fun at(local: String): Long = LocalDateTime.parse(local).atZone(cairo).toInstant().toEpochMilli()
    private fun hm(ms: Long) = Labels.clock(ms, cairo)

    private fun cp(type: CheckpointType, offset: Int, scheduled: String, status: CheckpointStatus = CheckpointStatus.PENDING, ack: String? = null) =
        Checkpoint(
            id = offset.toLong() + 1, transfusionId = 1, type = type, offsetMin = offset,
            scheduledAt = at(scheduled), ackAt = ack?.let { at(it) }, status = status,
        )

    @Test
    fun previewMatchesTheCardRow() {
        val row = TimelineBuilder.preview(at("2026-01-15T10:20"), ScheduleConfig(plannedDurationMin = 240))
        assertEquals(listOf("0", "15", "30", "1", "2", "3", "End"), row.map { it.number })
        assertEquals(listOf(null, "min", "min", "h", "h", "h", null), row.map { it.unit })
        assertEquals(listOf("10:20", "10:35", "10:50", "11:20", "12:20", "13:20", "14:20"), row.map { hm(it.time) })
        // Navy chips for start and hourly; outlined for 15, 30 and End — like the card.
        assertEquals(listOf(true, false, false, true, true, true, false), row.map { it.filledChip })
        assertEquals(DotStyle.FILLED, row.first().dot)
        assertEquals(DotStyle.FILLED, row.last().dot)
        assertEquals(DotStyle.HOLLOW, row[1].dot)
    }

    @Test
    fun liveRowShowsDoneOverdueAndUpcoming() {
        val t = Transfusion(id = 1, bedLabel = "Bed 12", colorIndex = 0, startAt = at("2026-01-15T10:20"), plannedDurationMin = 120, baselineConfirmed = true)
        val rows = listOf(
            cp(CheckpointType.START, 0, "2026-01-15T10:20", CheckpointStatus.ACKED, "2026-01-15T10:20"),
            cp(CheckpointType.M15, 15, "2026-01-15T10:35", CheckpointStatus.ACKED, "2026-01-15T10:37"),
            cp(CheckpointType.M30, 30, "2026-01-15T10:50", CheckpointStatus.MISSED),
            cp(CheckpointType.HOURLY, 60, "2026-01-15T11:20"),
            cp(CheckpointType.PLANNED_END, 120, "2026-01-15T12:20"),
            cp(CheckpointType.HOURLY, 180, "2026-01-15T13:20"), // beyond the planned end: hidden for now
            cp(CheckpointType.LIMIT_WARNING, 225, "2026-01-15T14:05"), // not on the card row
        )
        val row = TimelineBuilder.live(t, rows, now = at("2026-01-15T11:05"))
        assertEquals(listOf("0", "15", "30", "1", "End"), row.map { it.number })
        assertEquals(
            listOf(DotStyle.FILLED, DotStyle.FILLED, DotStyle.OVERDUE, DotStyle.HOLLOW, DotStyle.FILLED),
            row.map { it.dot },
        )
    }

    @Test
    fun liveRowShowsNextHourlyOncePastThePlannedEnd() {
        val t = Transfusion(id = 1, bedLabel = "Bed 12", colorIndex = 0, startAt = at("2026-01-15T10:20"), plannedDurationMin = 60, baselineConfirmed = true)
        val rows = listOf(
            cp(CheckpointType.PLANNED_END, 60, "2026-01-15T11:20", CheckpointStatus.ACKED, "2026-01-15T11:21"),
            cp(CheckpointType.HOURLY, 120, "2026-01-15T12:20"),
            cp(CheckpointType.HOURLY, 180, "2026-01-15T13:20"),
        )
        val row = TimelineBuilder.live(t, rows, now = at("2026-01-15T11:40"))
        assertEquals(listOf("End", "2"), row.map { it.number })
    }

    @Test
    fun statusOnTimeLateMissed() {
        val tol = 5
        val now = at("2026-01-15T12:00")
        assertEquals("on time", Timeliness.status(cp(CheckpointType.M15, 15, "2026-01-15T10:35", CheckpointStatus.ACKED, "2026-01-15T10:40"), now, tol).text)
        assertEquals("late +6 min", Timeliness.status(cp(CheckpointType.M15, 15, "2026-01-15T10:35", CheckpointStatus.ACKED, "2026-01-15T10:41"), now, tol).text)
        assertEquals("overdue", Timeliness.status(cp(CheckpointType.M30, 30, "2026-01-15T10:50", CheckpointStatus.MISSED), now, tol).text)
        assertEquals(
            "missed · documented +14 min",
            Timeliness.status(cp(CheckpointType.M30, 30, "2026-01-15T10:50", CheckpointStatus.MISSED, "2026-01-15T11:04"), now, tol).text,
        )
        assertEquals(StatusKind.UPCOMING, Timeliness.status(cp(CheckpointType.HOURLY, 120, "2026-01-15T12:20"), now, tol).kind)
        assertEquals(StatusKind.DUE, Timeliness.status(cp(CheckpointType.HOURLY, 60, "2026-01-15T11:20"), now, tol).kind)
    }

    @Test
    fun countdownWording() {
        assertEquals("now", Timeliness.countdown(10_000))
        assertEquals("in 1 min", Timeliness.countdown(40_000))
        assertEquals("in 12 min", Timeliness.countdown(12 * MINUTE_MS))
        assertEquals("in 1 h 05 min", Timeliness.countdown(65 * MINUTE_MS))
    }

    @Test
    fun pickedStartTimeIsNeverInTheFuture() {
        val now = at("2026-01-16T00:10")
        assertEquals(at("2026-01-15T23:50"), Labels.mostRecentPast(23, 50, now, cairo))
        assertEquals(at("2026-01-16T00:05"), Labels.mostRecentPast(0, 5, now, cairo))
    }

    @Test
    fun durationWording() {
        assertEquals("2 h", Labels.duration(120))
        assertEquals("45 min", Labels.duration(45))
        assertEquals("1 h 30 min", Labels.duration(90))
    }

    @Test
    fun endVsPromptNeverRings() {
        val end = cp(CheckpointType.END, 100, "2026-01-15T12:00")
        val now = at("2026-01-15T12:30")
        assertTrue(Grouping.dueNow(listOf(end), now).isEmpty())
        val plan = ArmPlanner.plan(listOf(end), now, preAlertMin = 0, missedTimeoutMin = 10)
        assertFalse(plan.ringNow)
        assertTrue(plan.requests.isEmpty())
    }
}

package app.transfusionvs.schedule

import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointStatus.ACKED
import app.transfusionvs.data.CheckpointStatus.CANCELLED
import app.transfusionvs.data.CheckpointStatus.MISSED
import app.transfusionvs.data.CheckpointStatus.PENDING
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointType.END
import app.transfusionvs.data.CheckpointType.HOURLY
import app.transfusionvs.data.CheckpointType.LIMIT_WARNING
import app.transfusionvs.data.CheckpointType.M15
import app.transfusionvs.data.CheckpointType.M30
import app.transfusionvs.data.CheckpointType.START
import app.transfusionvs.data.LogRow
import app.transfusionvs.data.TransfusionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class StatsTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private fun at(local: String): Long = LocalDateTime.parse(local).atZone(cairo).toInstant().toEpochMilli()

    private var nextId = 1L
    private fun row(
        type: CheckpointType,
        scheduled: String,
        status: CheckpointStatus,
        ack: String? = null,
        bed: String = "Bed 12",
        offset: Int = 15,
    ) = LogRow(
        id = nextId++, transfusionId = 1, type = type, offsetMin = offset, scheduledAt = at(scheduled),
        firedAt = if (status == PENDING) null else at(scheduled), ackAt = ack?.let { at(it) }, snoozeCount = 0,
        status = status, bedLabel = bed, colorIndex = 0, startAt = at("2026-01-15T10:20"), endAt = null,
        plannedDurationMin = 120, transfusionStatus = TransfusionStatus.RUNNING,
    )

    private val day = listOf(
        row(START, "2026-01-15T10:20", ACKED, "2026-01-15T10:20", offset = 0), // not counted
        row(M15, "2026-01-15T10:35", ACKED, "2026-01-15T10:38"), // on time (+3)
        row(M30, "2026-01-15T10:50", ACKED, "2026-01-15T10:58", offset = 30), // late (+8)
        row(HOURLY, "2026-01-15T11:20", MISSED, offset = 60), // missed, never documented
        row(HOURLY, "2026-01-15T12:20", MISSED, "2026-01-15T12:34", offset = 120), // missed, documented +14
        row(LIMIT_WARNING, "2026-01-15T14:05", ACKED, "2026-01-15T14:30", offset = 225), // not a VS check
        row(END, "2026-01-15T13:00", ACKED, "2026-01-15T13:02", offset = 160), // on time (+2)
        row(HOURLY, "2026-01-15T13:20", CANCELLED, offset = 180), // ignored
        row(HOURLY, "2026-01-15T14:20", PENDING, offset = 240), // not final yet
    )

    @Test
    fun summaryCountsOnTimeLateMissedAndAverageDelay() {
        val s = Stats.summarize(day, toleranceMin = 5)
        assertEquals(5, s.counted)
        assertEquals(2, s.onTime)
        assertEquals(1, s.late)
        assertEquals(2, s.missed)
        assertEquals(40, s.onTimePct)
        assertEquals((3 + 8 + 14 + 2) / 4.0, s.avgDelayMin!!, 0.001)
    }

    @Test
    fun toleranceChangesWhatCountsAsOnTime() {
        assertEquals(3, Stats.summarize(day, toleranceMin = 10).onTime)
    }

    @Test
    fun bucketsPerDayWeekAndMonth_newestFirst() {
        val rows = day + listOf(
            row(M15, "2026-01-17T09:15", ACKED, "2026-01-17T09:16"), // Saturday
            row(M15, "2026-02-02T08:15", ACKED, "2026-02-02T08:30"),
        )
        val days = Stats.buckets(rows, Period.DAY, 5, cairo, DayOfWeek.SATURDAY)
        assertEquals(listOf("Mon 2 Feb", "Sat 17 Jan", "Thu 15 Jan"), days.map { it.label })

        // Week starting Saturday (Egypt): Thu 15 Jan belongs to the week of Sat 10 Jan.
        val weeks = Stats.buckets(rows, Period.WEEK, 5, cairo, DayOfWeek.SATURDAY)
        assertEquals(listOf("Week of 31 Jan", "Week of 17 Jan", "Week of 10 Jan"), weeks.map { it.label })

        val months = Stats.buckets(rows, Period.MONTH, 5, cairo, DayOfWeek.SATURDAY)
        assertEquals(listOf("February 2026", "January 2026"), months.map { it.label })
        assertEquals(6, months[1].summary.counted)
        assertTrue(months[1].rows.none { it.status == CANCELLED || it.type == START || it.type == LIMIT_WARNING })
    }

    @Test
    fun csvHasHeaderOneLinePerCheckpointAndSafeCells() {
        val csv = CsvFormat.build(
            listOf(
                row(M15, "2026-01-15T10:35", ACKED, "2026-01-15T10:44", bed = "Bed 12, side room"),
                row(M30, "2026-01-15T10:50", MISSED, bed = "=HYPERLINK(\"x\")", offset = 30),
            ),
            toleranceMin = 5, zone = cairo,
        )
        val lines = csv.trimEnd().split("\r\n")
        assertEquals(CsvFormat.HEADER.joinToString(","), lines[0])
        assertEquals(3, lines.size)
        assertTrue(lines[1].startsWith("\"Bed 12, side room\",2026-01-15 10:20,,RUNNING,15-min check,2026-01-15 10:35,"))
        assertTrue(lines[1].endsWith(",9,late +9 min,0"))
        assertTrue(lines[2].startsWith("\"'=HYPERLINK(\"\"x\"\")\""))
        assertTrue(lines[2].contains(",missed,"))
    }
}

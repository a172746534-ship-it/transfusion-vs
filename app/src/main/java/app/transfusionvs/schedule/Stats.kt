package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.LogRow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// Pure Log & stats logic (unit tested). Test alarms never reach here (the query excludes them).

enum class Period { DAY, WEEK, MONTH }

data class Summary(
    /** Finished checkpoints that count: on time + late + missed. */
    val counted: Int,
    val onTime: Int,
    val late: Int,
    val missed: Int,
    /** Average minutes from scheduled time to "documented" (null if nothing documented). */
    val avgDelayMin: Double?,
) {
    val onTimePct: Int? get() = if (counted == 0) null else Math.round(onTime * 100.0 / counted).toInt()
}

data class Bucket(val start: LocalDate, val label: String, val rows: List<LogRow>, val summary: Summary)

fun LogRow.asCheckpoint() = Checkpoint(
    id = id, transfusionId = transfusionId, type = type, offsetMin = offsetMin, scheduledAt = scheduledAt,
    firedAt = firedAt, ackAt = ackAt, snoozeCount = snoozeCount, snoozeUntil = null, status = status,
)

object Stats {

    /**
     * What the stats measure: vital-signs documentation. The timed checks, the End check and the
     * End VS count; START (logged at Start) and the time-limit warning (not a VS check) don't.
     */
    val COUNTED_TYPES = setOf(
        CheckpointType.M15, CheckpointType.M30, CheckpointType.HOURLY, CheckpointType.PLANNED_END, CheckpointType.END,
    )

    fun summarize(rows: List<LogRow>, toleranceMin: Int): Summary {
        var onTime = 0
        var late = 0
        var missed = 0
        val delays = mutableListOf<Long>()
        val counted = rows.filter { it.type in COUNTED_TYPES && (it.status == CheckpointStatus.ACKED || it.status == CheckpointStatus.MISSED) }
        for (r in counted) {
            val delay = r.ackAt?.let { Math.floorDiv(it - r.scheduledAt, MINUTE_MS).coerceAtLeast(0) }
            if (delay != null) delays += delay
            when {
                r.status == CheckpointStatus.MISSED -> missed++
                delay != null && delay <= toleranceMin -> onTime++
                else -> late++
            }
        }
        return Summary(counted.size, onTime, late, missed, delays.takeIf { it.isNotEmpty() }?.average())
    }

    /** The log grouped per day / week / month (newest first), each with its own summary. */
    fun buckets(
        rows: List<LogRow>,
        period: Period,
        toleranceMin: Int,
        zone: ZoneId,
        firstDayOfWeek: DayOfWeek,
    ): List<Bucket> =
        rows.filter { it.type in COUNTED_TYPES && it.status != CheckpointStatus.CANCELLED && (it.status != CheckpointStatus.PENDING || it.type == CheckpointType.END) }
            .groupBy { keyOf(Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate(), period, firstDayOfWeek) }
            .toSortedMap(compareByDescending<LocalDate> { it })
            .map { (start, list) ->
                Bucket(start, labelOf(start, period), list.sortedByDescending { it.scheduledAt }, summarize(list, toleranceMin))
            }

    private fun keyOf(date: LocalDate, period: Period, firstDayOfWeek: DayOfWeek): LocalDate = when (period) {
        Period.DAY -> date
        Period.WEEK -> date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        Period.MONTH -> date.withDayOfMonth(1)
    }

    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val weekFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    private fun labelOf(start: LocalDate, period: Period): String = when (period) {
        Period.DAY -> start.format(dayFmt)
        Period.WEEK -> "Week of ${start.format(weekFmt)}"
        Period.MONTH -> start.format(monthFmt)
    }
}

object CsvFormat {

    val HEADER = listOf(
        "bed", "transfusion_start", "transfusion_end", "transfusion_status",
        "checkpoint", "scheduled", "rang", "documented", "delay_min", "result", "snoozes",
    )

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** The whole log as CSV, oldest first. Times in the phone's timezone. */
    fun build(rows: List<LogRow>, toleranceMin: Int, zone: ZoneId): String {
        fun time(ms: Long?) = ms?.let { Instant.ofEpochMilli(it).atZone(zone).format(stamp) } ?: ""
        val lines = mutableListOf(HEADER.joinToString(","))
        for (r in rows.sortedWith(compareBy({ it.scheduledAt }, { it.bedLabel }))) {
            val result = if (r.type == CheckpointType.START) "start logged" else Timeliness.status(r.asCheckpoint(), Long.MAX_VALUE, toleranceMin).text
            val delay = if (r.type == CheckpointType.START) null else r.ackAt?.let { Math.floorDiv(it - r.scheduledAt, MINUTE_MS) }
            lines += listOf(
                r.bedLabel,
                time(r.startAt),
                time(r.endAt),
                r.transfusionStatus.name,
                Labels.title(r.type, r.offsetMin),
                time(r.scheduledAt),
                time(r.firedAt),
                time(r.ackAt),
                delay?.toString() ?: "",
                result,
                r.snoozeCount.toString(),
            ).joinToString(",") { escape(it) }
        }
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    /** RFC 4180 quoting, plus a leading ' on text that a spreadsheet could run as a formula. */
    fun escape(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}

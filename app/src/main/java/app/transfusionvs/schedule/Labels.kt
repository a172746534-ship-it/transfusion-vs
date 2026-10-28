package app.transfusionvs.schedule

import app.transfusionvs.data.CheckpointType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Wording shared by the alarm screen, notifications and lists — same words as the card. */
object Labels {

    fun title(type: CheckpointType, offsetMin: Int): String = when (type) {
        CheckpointType.START -> "Start"
        CheckpointType.M15 -> "15-min check"
        CheckpointType.M30 -> "30-min check"
        CheckpointType.HOURLY -> "${offsetMin / 60}-h check"
        CheckpointType.PLANNED_END -> "End check"
        CheckpointType.LIMIT_WARNING -> "Time-limit warning"
        CheckpointType.END -> "End VS"
        CheckpointType.TEST -> "Test alarm"
    }

    fun subtitle(type: CheckpointType): String = when (type) {
        CheckpointType.PLANNED_END -> "Transfusion due to finish — measure end VS"
        CheckpointType.LIMIT_WARNING -> "Maximum duration is close — plan to finish"
        CheckpointType.TEST -> "If you can hear and see this, alarms work on this phone"
        else -> "Measure T · HR · RR · BP · SpO₂ → document in Cerner"
    }

    private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

    /** Clock time in the phone's current timezone. */
    fun clock(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(hhmm)

    /** "2 h", "90 min", "1 h 30 min". */
    fun duration(minutes: Int): String = when {
        minutes % 60 == 0 -> "${minutes / 60} h"
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    /**
     * The start-time picker gives a clock time; T0 is its most recent occurrence that is not in
     * the future (a transfusion that "started at 23:50" picked at 00:10 started yesterday).
     */
    fun mostRecentPast(hour: Int, minute: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val candidate = if (today.toInstant().toEpochMilli() > now) today.minusDays(1) else today
        return candidate.toInstant().toEpochMilli()
    }
}

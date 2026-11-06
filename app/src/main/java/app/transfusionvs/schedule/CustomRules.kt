package app.transfusionvs.schedule

import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.Settings
import java.text.Normalizer
import java.util.Locale

// Pure rules for custom timers (unit and property tested in CustomRulesTest and
// CustomSchedulePropertyTest). The series is in elapsed time: anchorAt + n × interval, so daylight
// saving and timezone changes never move it. A late acknowledgement never moves it either.
//
// An alarm: PENDING → RINGING (sound) → silenced at the missed timeout (sound stops, Overdue shows,
// still acknowledgeable) → MISSED when the next alarm fires. Done = ACKED.

object CustomRules {

    /** Interval buttons on the start screen; "Other" picks anything in [MIN_INTERVAL]..[MAX_INTERVAL]. */
    val INTERVALS = listOf(15, 30, 60, 120, 240, 360, 480, 720)
    const val MIN_INTERVAL = 15
    const val MAX_INTERVAL = 24 * 60
    const val OTHER_STEP = 5

    /** Finished alarms are kept this long (the phone keeps nothing long-term). */
    const val KEEP_MS = 24 * 60 * MINUTE_MS

    /** The first alarm of the series after both [after] and the timer's [CustomTimer.fromAt]. */
    fun next(timer: CustomTimer, after: Long): Long {
        val from = maxOf(after, timer.fromAt)
        val step = timer.intervalMin * MINUTE_MS
        val n = Math.floorDiv(from - timer.anchorAt, step) + 1
        return timer.anchorAt + maxOf(1L, n) * step
    }

    /** When an open alarm closes as MISSED: when the series' next alarm after it fires. */
    fun missAt(timer: CustomTimer, slot: CustomSlot): Long = next(timer, slot.slotAt)

    /**
     * New rows up to and including the first future alarm, so there is always a next one armed,
     * whatever the interval. Alarms that passed while the phone was off are created already MISSED
     * when the one after them has fired too. Nothing at or after the stop time, nothing older than a day.
     */
    fun materialize(timer: CustomTimer, existing: List<CustomSlot>, now: Long): List<CustomSlot> {
        if (timer.status != CustomTimerStatus.RUNNING) return emptyList()
        val last = existing.maxOfOrNull { it.slotAt }
        // The next alarm is already there.
        if (last != null && last > now) return emptyList()
        val have = existing.map { it.slotAt }.toSet()
        val out = mutableListOf<CustomSlot>()
        var at = next(timer, maxOf(last ?: timer.fromAt, now - KEEP_MS))
        while (true) {
            val stop = timer.stopAt
            if (stop != null && at >= stop) break
            if (at !in have) {
                val closed = next(timer, at) <= now
                out += CustomSlot(timerId = timer.id, slotAt = at, status = if (closed) CustomSlotStatus.MISSED else CustomSlotStatus.PENDING)
            }
            if (at > now) break
            at = next(timer, at)
        }
        return out
    }

    /** One refresh step at [now]: returns only the alarms that changed. */
    fun step(timer: CustomTimer, slots: List<CustomSlot>, now: Long, missedTimeoutMin: Int): List<CustomSlot> =
        slots.mapNotNull { s ->
            val open = s.status == CustomSlotStatus.RINGING || (s.status == CustomSlotStatus.PENDING && s.slotAt <= now + 1_000)
            if (!open) return@mapNotNull null
            val changed = when {
                // The next alarm fired: this one closes as MISSED (a late Done was possible until now).
                missAt(timer, s) <= now -> s.copy(status = CustomSlotStatus.MISSED, snoozeUntil = null)
                s.status == CustomSlotStatus.PENDING -> s.copy(status = CustomSlotStatus.RINGING, firedAt = now)
                // Missed timeout: the sound stops; it stays open (Overdue) until done or the next alarm.
                !s.silenced && (s.firedAt ?: now) + missedTimeoutMin * MINUTE_MS <= now -> s.copy(silenced = true, snoozeUntil = null)
                s.snoozeUntil != null && s.snoozeUntil <= now -> s.copy(snoozeUntil = null)
                else -> s
            }
            changed.takeIf { it != s }
        }

    /** Plays sound now: ringing, not silenced, not snoozed. */
    fun isAudible(s: CustomSlot, now: Long): Boolean =
        s.status == CustomSlotStatus.RINGING && !s.silenced && (s.snoozeUntil == null || s.snoozeUntil <= now)

    /** Open past the missed timeout: on the Overdue notification. */
    fun isOverdue(s: CustomSlot, now: Long, missedTimeoutMin: Int): Boolean =
        s.status == CustomSlotStatus.RINGING && (s.firedAt ?: now) + missedTimeoutMin * MINUTE_MS <= now

    /** Snooze only if it ends before the next alarm, and within the snooze limit. */
    fun canSnooze(timer: CustomTimer, s: CustomSlot, now: Long, snoozeMin: Int): Boolean =
        s.snoozeCount < Settings.MAX_SNOOZES && now + snoozeMin * MINUTE_MS < missAt(timer, s)

    /** The system alarms a timer needs. */
    fun arm(timer: CustomTimer, slots: List<CustomSlot>, now: Long, missedTimeoutMin: Int, preAlertMin: Int): ArmPlan {
        val requests = mutableListOf<AlarmRequest>()
        var ringNow = false
        for (s in slots) {
            when (s.status) {
                CustomSlotStatus.PENDING -> if (s.slotAt > now) {
                    requests += AlarmRequest(AlarmKind.CHECKPOINT, s.id, s.slotAt, custom = true)
                    val pre = s.slotAt - preAlertMin * MINUTE_MS
                    if (preAlertMin > 0 && pre > now) requests += AlarmRequest(AlarmKind.PREALERT, s.id, pre, custom = true)
                } else {
                    ringNow = true
                }
                CustomSlotStatus.RINGING -> {
                    requests += AlarmRequest(AlarmKind.MISS, s.id, missAt(timer, s), custom = true)
                    val timeoutAt = (s.firedAt ?: now) + missedTimeoutMin * MINUTE_MS
                    if (timeoutAt > now) requests += AlarmRequest(AlarmKind.TIMEOUT, s.id, timeoutAt, custom = true)
                    val snooze = s.snoozeUntil
                    if (snooze != null && snooze > now) requests += AlarmRequest(AlarmKind.SNOOZE, s.id, snooze, custom = true)
                    if (isAudible(s, now)) ringNow = true
                }
                else -> Unit
            }
        }
        return ArmPlan(requests, ringNow)
    }

    /** A timer whose stop time has passed and has nothing open is stopped. */
    fun shouldAutoStop(timer: CustomTimer, slots: List<CustomSlot>, now: Long): Boolean {
        val stop = timer.stopAt ?: return false
        return timer.status == CustomTimerStatus.RUNNING && stop <= now &&
            slots.none { it.status == CustomSlotStatus.RINGING || (it.status == CustomSlotStatus.PENDING && it.slotAt < stop) }
    }

    /**
     * The interval was edited: the new series starts from the last Done (or the start), on the
     * whole minute, and nothing from before [now] rings.
     */
    fun edited(timer: CustomTimer, slots: List<CustomSlot>, intervalMin: Int, now: Long): CustomTimer {
        if (intervalMin == timer.intervalMin) return timer
        val lastDone = slots.filter { it.status == CustomSlotStatus.ACKED }.mapNotNull { it.ackAt }.maxOrNull()
        val anchor = ScheduleCalculator.truncateToMinute(lastDone ?: timer.startAt)
        return timer.copy(intervalMin = intervalMin, anchorAt = anchor, fromAt = maxOf(now, timer.fromAt))
    }
}

/** The task name: typed freely, but kept short, single-line and never empty or invisible. */
object CustomName {
    const val MAX = 30

    private val INVISIBLE = setOf(0x200B, 0x200C, 0x200D, 0x200E, 0x200F, 0x061C, 0xFEFF)

    /** As typed: control characters (line breaks too) go, and input past [MAX] characters is ignored. */
    fun clean(input: String): String {
        val sb = StringBuilder()
        input.codePoints().forEach { if (Character.getType(it) != Character.CONTROL.toInt()) sb.appendCodePoint(it) }
        val s = sb.toString()
        // Counted from the first visible character: leading spaces are trimmed on save anyway.
        val lead = s.length - s.trimStart().length
        val body = s.substring(lead)
        return if (body.codePointCount(0, body.length) <= MAX) s else s.substring(0, lead + body.offsetByCodePoints(0, MAX))
    }

    /** 1–30 characters after trimming, and something visible in it. */
    fun valid(name: String): Boolean {
        val t = name.trim()
        val n = t.codePointCount(0, t.length)
        return n in 1..MAX && t.codePoints().anyMatch { !Character.isWhitespace(it) && !Character.isSpaceChar(it) && it !in INVISIBLE }
    }

    /** For "the same name": NFC, trimmed, runs of spaces as one, case ignored. */
    fun key(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}

/** One custom alarm's status in the timer's list: the same words and pills as the other types. */
object CustomStatus {

    fun of(s: CustomSlot, now: Long, toleranceMin: Int, missedTimeoutMin: Int): StatusView = when (s.status) {
        CustomSlotStatus.PENDING ->
            if (s.slotAt <= now) StatusView(StatusKind.DUE, "due now") else StatusView(StatusKind.UPCOMING, "upcoming")
        CustomSlotStatus.RINGING -> when {
            CustomRules.isOverdue(s, now, missedTimeoutMin) -> StatusView(StatusKind.MISSED, "overdue")
            s.snoozeUntil != null && s.snoozeUntil > now -> StatusView(StatusKind.SNOOZED, "snoozed")
            else -> StatusView(StatusKind.RINGING, "ringing")
        }
        CustomSlotStatus.ACKED -> {
            val late = Math.floorDiv((s.ackAt ?: s.slotAt) - s.slotAt, MINUTE_MS)
            if (late <= toleranceMin) StatusView(StatusKind.ON_TIME, "on time") else StatusView(StatusKind.LATE, "late +$late min")
        }
        CustomSlotStatus.MISSED -> StatusView(StatusKind.MISSED, "missed")
        CustomSlotStatus.CANCELLED -> StatusView(StatusKind.CANCELLED, "cancelled")
    }
}

package app.transfusionvs.schedule

import app.transfusionvs.data.Position
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Pure repositioning rules (unit tested in TurnGridTest).
// Turns run on a FIXED local ward clock: slots are multiples of the interval counted from 00:00
// (every 2 h: 00:00, 02:00 … 08:00, 10:00 …). A late turn never moves the grid.

object TurnGrid {

    /** Interval chips. All three keep 08:00 and 20:00 on the grid. */
    val INTERVALS = listOf(60, 120, 240)

    /** If the next slot is closer than this when a plan starts, the first turn is the slot after it. */
    const val MIN_LEAD_MIN = 30

    /** "Remind me in 30 min". */
    const val REMIND_MIN = 30

    /** Shift changes (for "End of my shift"). */
    val SHIFT_CHANGES: List<LocalTime> = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))

    /**
     * The first grid slot strictly after [after], in the phone's local time.
     * A slot that doesn't exist that day (DST spring-forward) is skipped; a repeated hour
     * (DST fall-back) yields the slot once.
     */
    fun nextSlotAfter(after: Long, intervalMin: Int, zone: ZoneId): Long {
        require(1440 % intervalMin == 0) { "interval must divide a day" }
        val day = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
        for (d in 0..2) {
            for (slot in slotsOfDay(day.plusDays(d.toLong()), intervalMin, zone)) {
                if (slot > after) return slot
            }
        }
        error("no slot found") // unreachable: every day has slots
    }

    /** All existing slots of one local day. */
    private fun slotsOfDay(day: LocalDate, intervalMin: Int, zone: ZoneId): List<Long> =
        (0 until 1440 / intervalMin).mapNotNull { k ->
            val local = day.atStartOfDay().plusMinutes(k.toLong() * intervalMin)
            val zoned = local.atZone(zone)
            // atZone() shifts a non-existent time forward; such a slot is skipped instead.
            if (zoned.toLocalDateTime() == local) zoned.toInstant().toEpochMilli() else null
        }

    /** First turn of a plan started at [start]: the next slot, unless it is under 30 min away. */
    fun firstSlot(start: Long, intervalMin: Int, zone: ZoneId): Long {
        val next = nextSlotAfter(start, intervalMin, zone)
        return if (next - start < MIN_LEAD_MIN * MINUTE_MS) nextSlotAfter(next, intervalMin, zone) else next
    }

    /** The choices offered for the first turn: the default and the slot after it. */
    fun firstSlotChoices(start: Long, intervalMin: Int, zone: ZoneId): List<Long> {
        val next = nextSlotAfter(start, intervalMin, zone)
        val following = nextSlotAfter(next, intervalMin, zone)
        return listOf(next, following)
    }

    /** "End of my shift" = the next 08:00 or 20:00 strictly after [now]. */
    fun endOfShift(now: Long, zone: ZoneId): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (0..1).flatMap { d -> SHIFT_CHANGES.map { LocalDateTime.of(today.plusDays(d.toLong()), it) } }
            .map { it.atZone(zone).toInstant().toEpochMilli() }
            .filter { it > now }
            .min()
    }

    /**
     * The plan's slots from [firstSlot] (inclusive) up to [until] (inclusive), stopping before
     * [stopAt] if set — no alarm at or after the stop time.
     */
    fun slots(firstSlot: Long, intervalMin: Int, stopAt: Long?, until: Long, zone: ZoneId): List<Long> {
        val out = mutableListOf<Long>()
        // After a timezone change the stored first slot may be off the new local grid: the ward clock wins.
        var slot = if (isOnGrid(firstSlot, intervalMin, zone)) firstSlot else nextSlotAfter(firstSlot, intervalMin, zone)
        while (slot <= until && (stopAt == null || slot < stopAt)) {
            out += slot
            slot = nextSlotAfter(slot, intervalMin, zone)
        }
        return out
    }

    /** True when [at] is a whole multiple of the interval on the local clock in [zone]. */
    fun isOnGrid(at: Long, intervalMin: Int, zone: ZoneId): Boolean {
        val local = Instant.ofEpochMilli(at).atZone(zone).toLocalTime()
        return local.second == 0 && local.nano == 0 && (local.hour * 60 + local.minute) % intervalMin == 0
    }

    /** A slot can be acknowledged late until the next slot fires; then it closes as MISSED. */
    fun missAt(slotAt: Long, intervalMin: Int, zone: ZoneId): Long = nextSlotAfter(slotAt, intervalMin, zone)

    /** "Remind me in 30 min" is offered only if it lands before the next slot. */
    fun canRemind(now: Long, nextSlotAt: Long): Boolean = now + REMIND_MIN * MINUTE_MS < nextSlotAt

    /**
     * The suggested next position: the next one in the bed's rotation after [current].
     * Excluded positions are not in the rotation, so they are never suggested.
     */
    fun nextPosition(current: Position, rotation: List<Position>): Position {
        if (rotation.isEmpty()) return current
        val i = rotation.indexOf(current)
        return if (i < 0) rotation.first() else rotation[(i + 1) % rotation.size]
    }

    /** Longest stretch without a turn, from actual turn times (ack times), up to [now]. */
    fun longestGapMs(turnTimes: List<Long>, planStart: Long, now: Long): Long {
        val points = (listOf(planStart) + turnTimes.filter { it in planStart..now }.sorted() + now)
        return points.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0
    }
}

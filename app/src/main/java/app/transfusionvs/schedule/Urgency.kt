package app.transfusionvs.schedule

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.TurnSlotStatus

// What a bed card says at a glance (pure, unit tested in UrgencyTest). The UI's temperature:
// calm → due soon → due now → overdue, told by colour AND shape AND text together.

enum class Urgency { CALM, SOON, NOW, OVERDUE }

enum class BedType { TRANSFUSION, TURN }

/** Where the rail stands: [done] dots behind, the current one next, [progress] of the way to it. */
data class RailState(val total: Int, val done: Int, val progress: Float)

data class BedView(
    val type: BedType,
    val id: Long,
    val bed: String,
    /** The clock time this card is about; null when nothing is scheduled. */
    val time: Long?,
    val what: String,
    val urgency: Urgency,
    val stateText: String,
    val rail: RailState,
    /** Stopped for a reaction: the most serious state, above everything else. */
    val stoppedForReaction: Boolean = false,
    /** A stopped turning plan: quiet, at the bottom, kept so it can be restarted. */
    val idle: Boolean = false,
    /** Checkpoints a "Measured & documented ✓" on the card would acknowledge. */
    val ackIds: List<Long> = emptyList(),
    /** A turn is open: the card offers to open the round. */
    val roundOpen: Boolean = false,
)

object BedViews {

    /** Under this many minutes away a check is "due soon". */
    const val SOON_MIN = 15

    fun rail(times: List<Long>, firstOpen: Int?, now: Long): RailState {
        if (times.isEmpty()) return RailState(0, 0, 0f)
        val current = firstOpen ?: times.indexOfFirst { it > now }.let { if (it < 0) times.lastIndex else it }
        val progress = when {
            firstOpen != null || current == 0 -> if (firstOpen != null) 1f else 0f
            else -> {
                val from = times[current - 1]
                val to = times[current]
                if (to <= from) 1f else ((now - from).toFloat() / (to - from)).coerceIn(0f, 1f)
            }
        }
        return RailState(times.size, current, progress)
    }

    /** The alarm screen's rail: where this check sits on the bed's plan. */
    fun alarmRail(offsetMin: Int, plannedDurationMin: Int): RailState {
        val offsets = ScheduleCalculator.plan(ScheduleConfig(plannedDurationMin), plannedDurationMin)
            .filter { it.type != CheckpointType.LIMIT_WARNING }
            .map { it.offsetMin }
        val all = (offsets + offsetMin).distinct().sorted()
        return RailState(all.size, all.indexOf(offsetMin), 1f)
    }

    private fun lateText(at: Long, now: Long) = "OVERDUE +${((now - at).coerceAtLeast(0)) / MINUTE_MS} min"

    private fun upcoming(at: Long, now: Long): Pair<Urgency, String> {
        val left = at - now
        return when {
            left <= 0 -> Urgency.NOW to "DUE NOW"
            left <= SOON_MIN * MINUTE_MS -> Urgency.SOON to "SOON · ${(left + MINUTE_MS - 1) / MINUTE_MS} min"
            else -> Urgency.CALM to Timeliness.countdown(left)
        }
    }

    fun of(item: TransfusionWithCheckpoints, now: Long): BedView {
        val t = item.transfusion
        val rows = item.checkpoints
        val points = TimelineBuilder.live(t, rows, now)
        val firstOpen = points.indexOfFirst { it.dot == DotStyle.OVERDUE }.takeIf { it >= 0 }
        val rail = rail(points.map { it.time }, firstOpen, now)
        val ringing = rows.filter { it.status == CheckpointStatus.RINGING }.sortedBy { it.scheduledAt }
        val missed = rows.filter { it.status == CheckpointStatus.MISSED && it.ackAt == null }.sortedBy { it.scheduledAt }
        val endPrompt = rows.firstOrNull { it.type == CheckpointType.END && it.status == CheckpointStatus.PENDING }
        val next = rows.filter { it.status == CheckpointStatus.PENDING && it.type.rings }.minByOrNull { it.scheduledAt }
        fun titles(list: List<Checkpoint>) = list.joinToString(" + ") { Labels.title(it.type, it.offsetMin) }
        val base = BedView(BedType.TRANSFUSION, t.id, t.bedLabel, null, "", Urgency.CALM, "", rail)
        return when {
            t.status == TransfusionStatus.STOPPED_REACTION -> base.copy(
                time = t.endAt, what = "Transfusion stopped", urgency = Urgency.OVERDUE,
                stateText = "STOPPED — REACTION", stoppedForReaction = true, ackIds = missed.map { it.id },
            )
            missed.isNotEmpty() -> base.copy(
                time = missed.first().scheduledAt, what = titles(missed), urgency = Urgency.OVERDUE,
                stateText = lateText(missed.first().scheduledAt, now), ackIds = missed.map { it.id },
            )
            ringing.isNotEmpty() -> {
                val snoozedUntil = ringing.mapNotNull { it.snoozeUntil }.filter { it > now }.minOrNull()
                base.copy(
                    time = ringing.first().scheduledAt, what = titles(ringing), urgency = Urgency.NOW,
                    stateText = if (snoozedUntil != null && ringing.all { (it.snoozeUntil ?: 0) > now }) "SNOOZED · ${Labels.clock(snoozedUntil)}" else "DUE NOW",
                )
            }
            endPrompt != null -> base.copy(
                time = endPrompt.scheduledAt, what = "Measure, then document in Cerner", urgency = Urgency.NOW,
                stateText = "END VS", ackIds = listOf(endPrompt.id),
            )
            next != null && t.status == TransfusionStatus.RUNNING -> {
                val (u, text) = upcoming(next.scheduledAt, now)
                base.copy(time = next.scheduledAt, what = Labels.title(next.type, next.offsetMin), urgency = u, stateText = text)
            }
            t.status == TransfusionStatus.ENDED -> base.copy(time = t.endAt, what = "Transfusion ended", stateText = "Ended", idle = true)
            else -> base.copy(what = "Running", stateText = "No check due")
        }
    }

    fun of(item: TurnPlanWithSlots, now: Long, missedTimeoutMin: Int): BedView {
        val p = item.plan
        val slots = item.slots
        val rotation = Position.parseList(p.rotation)
        val points = TurnTimeline.live(p, slots, now)
        val firstOpen = points.indexOfFirst { it.dot == DotStyle.OVERDUE && it.time >= (slots.filter { s -> s.status == TurnSlotStatus.RINGING }.minOfOrNull { s -> s.slotAt } ?: Long.MAX_VALUE) }.takeIf { it >= 0 }
        val rail = rail(points.map { it.time }, firstOpen, now)
        val open = slots.filter { it.status == TurnSlotStatus.RINGING }.sortedBy { it.slotAt }
        val next = slots.filter { it.status == TurnSlotStatus.PENDING && it.slotAt > now }.minByOrNull { it.slotAt }
        val turn = "${p.currentPosition.label} → ${TurnGrid.nextPosition(p.currentPosition, rotation).label}"
        val base = BedView(BedType.TURN, p.id, p.bedLabel, null, turn, Urgency.CALM, "", rail)
        return when {
            p.status == TurnPlanStatus.STOPPED -> base.copy(what = "Lying ${p.currentPosition.label}", stateText = "Stopped", idle = true)
            open.isNotEmpty() -> {
                val s = open.first()
                val overdue = TurnRules.isOverdue(s, now, missedTimeoutMin)
                val snoozed = s.snoozeUntil != null && s.snoozeUntil > now
                base.copy(
                    time = s.slotAt,
                    what = "${s.positionFrom.label} → ${TurnGrid.nextPosition(s.positionFrom, rotation).label}",
                    urgency = if (overdue) Urgency.OVERDUE else Urgency.NOW,
                    stateText = when {
                        overdue -> lateText(s.slotAt, now)
                        snoozed -> "SNOOZED · ${Labels.clock(s.snoozeUntil!!)}"
                        else -> "DUE NOW"
                    },
                    roundOpen = true,
                )
            }
            next != null -> {
                val (u, text) = upcoming(next.slotAt, now)
                base.copy(time = next.slotAt, urgency = u, stateText = text)
            }
            else -> base.copy(what = "Lying ${p.currentPosition.label}", stateText = "No turn due")
        }
    }

    private fun rank(b: BedView) = when {
        b.stoppedForReaction -> 0
        b.idle -> 5
        b.urgency == Urgency.OVERDUE -> 1
        b.urgency == Urgency.NOW -> 2
        b.urgency == Urgency.SOON -> 3
        else -> 4
    }

    /** Most urgent first, then soonest. */
    fun sorted(beds: List<BedView>): List<BedView> =
        beds.sortedWith(compareBy<BedView> { rank(it) }.thenBy { it.time ?: Long.MAX_VALUE }.thenBy { it.bed })

    /** "1 overdue · 1 due now", "All on time · next 11:20", "Nothing running". */
    fun summary(beds: List<BedView>): String {
        val active = beds.filter { !it.idle }
        if (active.isEmpty()) return "Nothing running"
        val stopped = active.count { it.stoppedForReaction }
        val overdue = active.count { it.urgency == Urgency.OVERDUE && !it.stoppedForReaction }
        val now = active.count { it.urgency == Urgency.NOW }
        val parts = listOfNotNull(
            "$stopped stopped".takeIf { stopped > 0 },
            "$overdue overdue".takeIf { overdue > 0 },
            "$now due now".takeIf { now > 0 },
        )
        if (parts.isNotEmpty()) return parts.joinToString(" · ")
        val next = active.mapNotNull { it.time }.minOrNull()
        return if (next == null) "All on time" else "All on time · next ${Labels.clock(next)}"
    }

    /** "6 BEDS RUNNING". */
    fun running(beds: List<BedView>): String {
        val n = beds.count { !it.idle && !it.stoppedForReaction }
        return when (n) {
            0 -> "NO BEDS RUNNING"
            1 -> "1 BED RUNNING"
            else -> "$n BEDS RUNNING"
        }
    }
}

/** The night theme's rule: by the clock (default 20:00–08:00), the phone's setting, or fixed. */
object NightRule {
    const val AUTO_CLOCK = 0
    const val FOLLOW_SYSTEM = 1
    const val ALWAYS_DAY = 2
    const val ALWAYS_NIGHT = 3

    fun isNight(mode: Int, hour: Int, startHour: Int, endHour: Int, systemDark: Boolean): Boolean = when (mode) {
        FOLLOW_SYSTEM -> systemDark
        ALWAYS_DAY -> false
        ALWAYS_NIGHT -> true
        else -> if (startHour > endHour) hour >= startHour || hour < endHour else hour in startHour until endHour
    }
}

/** The ward's bed list: "1-12, 14, ICU 3" typed once in Settings. Bed numbers, no names. */
object BedInput {
    const val MAX_LABEL = 16
    private const val MAX_RANGE = 100

    /** A plain number means "Bed N"; a range adds each bed; anything else is kept as typed. */
    fun parse(text: String): List<String> = text.split(',', '\n', ';').map { it.trim() }.filter { it.isNotEmpty() }.flatMap { part ->
        val range = Regex("""^(\d{1,4})\s*[-–]\s*(\d{1,4})$""").find(part)
        when {
            range != null -> {
                val from = range.groupValues[1].toInt()
                val to = range.groupValues[2].toInt()
                if (from <= to && to - from < MAX_RANGE) (from..to).map { "Bed $it" } else emptyList()
            }
            part.all { it.isDigit() } -> listOf("Bed $part".take(MAX_LABEL))
            else -> listOf(part.take(MAX_LABEL))
        }
    }.distinct()

    /** Natural order: Bed 2 before Bed 10. */
    fun sorted(labels: List<String>): List<String> = labels.sortedWith(
        compareBy<String> { it.replace(Regex("""\d+"""), "") }.thenBy { Regex("""\d+""").find(it)?.value?.toIntOrNull() ?: 0 }.thenBy { it },
    )
}

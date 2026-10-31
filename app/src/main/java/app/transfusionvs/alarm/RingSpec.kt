package app.transfusionvs.alarm

import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.Position
import app.transfusionvs.data.Settings
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.schedule.TurnRules
import java.time.ZoneId

/** One bed on the alarm: it gets its own ✓ button that acknowledges all of its rows. */
data class RingBed(
    val transfusionId: Long,
    val bedLabel: String,
    val colorIndex: Int,
    val isTest: Boolean,
    val checkpointIds: List<Long>,
    /** e.g. "15-min check", or "2-h check + Time-limit warning". */
    val what: String,
    val subtitle: String,
) {
    val title: String get() = if (isTest) what else "$what — $bedLabel"
    val shortLabel: String get() = if (isTest) "Test" else bedLabel
}

/** Everything that is sounding right now, as shown on the alarm screen and notification. */
data class RingSpec(
    val beds: List<RingBed>,
    val canSnooze: Boolean,
    val snoozeMin: Int,
    val soundOn: Boolean,
    val soundUri: String?,
    val missedTimeoutMin: Int,
    /** Open turning round (listed below the transfusion beds), if any. */
    val round: RoundUi? = null,
    /** Turning sound (null = the bundled chime). */
    val turnSoundUri: String? = null,
) {
    /** For what shows outside the app: every bed label through [mask]. */
    fun masked(mask: (String) -> String) = copy(
        beds = beds.map { it.copy(bedLabel = mask(it.bedLabel)) },
        round = round?.masked(mask),
    )

    /** Only turns are sounding: play the turning sound and vibration, not the transfusion alarm. */
    val turnOnly: Boolean get() = beds.isEmpty() && round != null

    val title: String
        get() = when {
            beds.isEmpty() && round != null -> roundTitle(round)
            beds.size == 1 -> beds[0].title
            else -> "${beds.size} beds due now"
        }

    companion object {
        fun build(
            audible: List<CheckpointWithBed>,
            canSnooze: Boolean,
            snoozeMin: Int,
            soundOn: Boolean,
            soundUri: String?,
            missedTimeoutMin: Int,
            round: RoundUi? = null,
            turnSoundUri: String? = null,
        ) = RingSpec(
            beds = groupByBed(audible),
            canSnooze = canSnooze,
            snoozeMin = snoozeMin,
            soundOn = soundOn,
            soundUri = soundUri,
            missedTimeoutMin = missedTimeoutMin,
            round = round,
            turnSoundUri = turnSoundUri,
        )

        fun roundTitle(r: RoundUi) =
            "Turning round · ${Labels.clock(r.slotAt)} · ${r.beds.size} bed${if (r.beds.size > 1) "s" else ""}"

        fun groupByBed(rows: List<CheckpointWithBed>): List<RingBed> =
            rows.groupBy { it.transfusionId }.values.map { bedRows ->
                val first = bedRows.first()
                RingBed(
                    transfusionId = first.transfusionId,
                    bedLabel = first.bedLabel,
                    colorIndex = first.colorIndex,
                    isTest = first.isTest,
                    checkpointIds = bedRows.map { it.id },
                    what = bedRows.joinToString(" + ") { Labels.title(it.type, it.offsetMin) },
                    subtitle = Labels.subtitle(
                        bedRows.firstOrNull { it.type != CheckpointType.HOURLY }?.type ?: first.type,
                    ),
                )
            }
    }
}

// ---- Repositioning round (one alarm per ward-clock slot, all due beds together) ----

/** One bed on the round: where the patient lies and the suggested next position. */
data class RoundBed(
    val planId: Long,
    val slotId: Long,
    val bedLabel: String,
    val colorIndex: Int,
    val from: Position,
    val suggested: Position,
    /** The bed's allowed positions (excluded ones are never offered). */
    val allowed: List<Position>,
)

data class RoundUi(
    val slotAt: Long,
    val beds: List<RoundBed>,
    /** "Start round" tapped: the sound is off and the checklist shows. */
    val started: Boolean,
    val canSnooze: Boolean,
    val snoozedUntil: Long?,
    /** "Remind me in 30 min" — only if it lands before the next slot. */
    val canRemind: Boolean,
) {
    fun masked(mask: (String) -> String) = copy(beds = beds.map { it.copy(bedLabel = mask(it.bedLabel)) })
}

/** Which secondary action is open on a round row. */
enum class RoundChoice { OTHER_POSITION, CANT_TURN }

/** Builds the round (all open turn slots, any bed) from the database rows. */
object RoundBuilder {

    fun build(plans: List<TurnPlanWithSlots>, now: Long, zone: ZoneId): RoundUi? {
        val open = plans.flatMap { p -> p.slots.filter { it.status == TurnSlotStatus.RINGING }.map { p.plan to it } }
        if (open.isEmpty()) return null
        val beds = open.map { (plan, s) ->
            val rotation = Position.parseList(plan.rotation)
            RoundBed(
                planId = plan.id, slotId = s.id, bedLabel = plan.bedLabel, colorIndex = plan.colorIndex,
                from = s.positionFrom, suggested = TurnGrid.nextPosition(s.positionFrom, rotation), allowed = rotation,
            )
        }
        val audible = open.filter { TurnRules.isAudible(it.second, now) }
        val started = open.all { it.second.silenced }
        val nextSlot = open.minOf { (plan, s) -> TurnGrid.missAt(s.slotAt, plan.intervalMin, zone) }
        return RoundUi(
            slotAt = open.maxOf { it.second.slotAt },
            beds = beds,
            started = started,
            canSnooze = audible.isNotEmpty() && audible.all { it.second.snoozeCount < Settings.MAX_SNOOZES },
            snoozedUntil = open.mapNotNull { it.second.snoozeUntil }.filter { it > now }.minOrNull(),
            canRemind = started && TurnGrid.canRemind(now, nextSlot) && open.none { it.second.remindAt != null },
        )
    }
}

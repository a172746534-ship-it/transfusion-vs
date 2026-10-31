package app.transfusionvs.alarm

import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.Position
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus

/** One thing to put back when an action is undone. Status and times only. */
sealed interface UndoStep {
    data class RestoreCheckpoint(
        val id: Long,
        val status: CheckpointStatus,
        val firedAt: Long?,
        val ackAt: Long?,
        val snoozeCount: Int,
        val snoozeUntil: Long?,
    ) : UndoStep

    data class RestoreSlot(
        val id: Long,
        val status: TurnSlotStatus,
        val firedAt: Long?,
        val ackAt: Long?,
        val positionFrom: Position,
        val positionTo: Position?,
        val skipReason: SkipReason?,
        val snoozeCount: Int,
        val snoozeUntil: Long?,
        val silenced: Boolean,
        val remindAt: Long?,
    ) : UndoStep

    data class RestoreTransfusion(val id: Long, val status: TransfusionStatus, val endAt: Long?) : UndoStep
    data class RestorePlan(val id: Long, val status: TurnPlanStatus, val stopAt: Long?, val current: Position) : UndoStep
    data class DeleteCheckpoint(val id: Long) : UndoStep
    data class DeleteTransfusion(val id: Long) : UndoStep
    data class DeletePlan(val id: Long) : UndoStep

    companion object {
        fun of(c: Checkpoint) = RestoreCheckpoint(c.id, c.status, c.firedAt, c.ackAt, c.snoozeCount, c.snoozeUntil)
        fun of(s: TurnSlot) = RestoreSlot(
            s.id, s.status, s.firedAt, s.ackAt, s.positionFrom, s.positionTo, s.skipReason, s.snoozeCount, s.snoozeUntil, s.silenced, s.remindAt,
        )
        fun of(t: Transfusion) = RestoreTransfusion(t.id, t.status, t.endAt)
        fun of(p: TurnPlan) = RestorePlan(p.id, p.status, p.stopAt, p.currentPosition)
    }
}

/** Plain text so it stays readable and needs no library: one step per line, fields tab-separated. */
object UndoCodec {
    private const val NULL = "-"

    private fun Long?.enc() = this?.toString() ?: NULL
    private fun String.long(): Long? = if (this == NULL) null else toLong()
    private inline fun <reified E : Enum<E>> String.enumOrNull(): E? = if (this == NULL) null else enumValueOf<E>(this)

    fun encode(steps: List<UndoStep>): String = steps.joinToString("\n") { s ->
        when (s) {
            is UndoStep.RestoreCheckpoint -> listOf("C", s.id, s.status.name, s.firedAt.enc(), s.ackAt.enc(), s.snoozeCount, s.snoozeUntil.enc())
            is UndoStep.RestoreSlot -> listOf(
                "S", s.id, s.status.name, s.firedAt.enc(), s.ackAt.enc(), s.positionFrom.name, s.positionTo?.name ?: NULL,
                s.skipReason?.name ?: NULL, s.snoozeCount, s.snoozeUntil.enc(), s.silenced, s.remindAt.enc(),
            )
            is UndoStep.RestoreTransfusion -> listOf("T", s.id, s.status.name, s.endAt.enc())
            is UndoStep.RestorePlan -> listOf("P", s.id, s.status.name, s.stopAt.enc(), s.current.name)
            is UndoStep.DeleteCheckpoint -> listOf("DC", s.id)
            is UndoStep.DeleteTransfusion -> listOf("DT", s.id)
            is UndoStep.DeletePlan -> listOf("DP", s.id)
        }.joinToString("\t")
    }

    fun decode(text: String): List<UndoStep> = text.lineSequence().filter { it.isNotBlank() }.map { line ->
        val f = line.split('\t')
        when (f[0]) {
            "C" -> UndoStep.RestoreCheckpoint(f[1].toLong(), enumValueOf(f[2]), f[3].long(), f[4].long(), f[5].toInt(), f[6].long())
            "S" -> UndoStep.RestoreSlot(
                f[1].toLong(), enumValueOf(f[2]), f[3].long(), f[4].long(), enumValueOf(f[5]), f[6].enumOrNull<Position>(),
                f[7].enumOrNull<SkipReason>(), f[8].toInt(), f[9].long(), f[10].toBoolean(), f[11].long(),
            )
            "T" -> UndoStep.RestoreTransfusion(f[1].toLong(), enumValueOf(f[2]), f[3].long())
            "P" -> UndoStep.RestorePlan(f[1].toLong(), enumValueOf(f[2]), f[3].long(), enumValueOf(f[4]))
            "DC" -> UndoStep.DeleteCheckpoint(f[1].toLong())
            "DT" -> UndoStep.DeleteTransfusion(f[1].toLong())
            "DP" -> UndoStep.DeletePlan(f[1].toLong())
            else -> error("unknown undo step: ${f[0]}")
        }
    }.toList()
}

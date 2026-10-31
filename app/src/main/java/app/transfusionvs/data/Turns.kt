package app.transfusionvs.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// Repositioning rounds. The patient by MRN only (`bedLabel`): no names or clinical values.

enum class Position(val label: String) {
    LEFT_30("Left 30°"),
    BACK("Back"),
    RIGHT_30("Right 30°"),
    PRONE("Prone"),
    HOB_UP("HOB up"),
    ;

    companion object {
        /** Default rotation: Left → Back → Right → Left … */
        val DEFAULT_ROTATION = listOf(LEFT_30, BACK, RIGHT_30)

        fun parseList(csv: String): List<Position> =
            csv.split(',').mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }

        fun toCsv(list: List<Position>): String = list.joinToString(",") { it.name }
    }
}

/** "Can't turn" reasons — logged as SKIPPED, not MISSED. */
enum class SkipReason(val label: String) {
    OFF_UNIT("Off unit"),
    PROCEDURE("Procedure"),
    ASLEEP("Asleep/comfort"),
    UNSTABLE("Unstable"),
    OTHER("Other"),
}

enum class TurnPlanStatus { RUNNING, STOPPED }

enum class TurnSlotStatus { PENDING, RINGING, ACKED, MISSED, SKIPPED, CANCELLED }

@Entity(tableName = "turn_plan")
data class TurnPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bedLabel: String,
    val colorIndex: Int,
    /** 60, 120 or 240 minutes on the ward-clock grid. */
    val intervalMin: Int,
    val startAt: Long,
    val firstSlotAt: Long,
    /** No slot at or after this time. Null = no stop time. */
    val stopAt: Long? = null,
    val status: TurnPlanStatus = TurnPlanStatus.RUNNING,
    /** Allowed positions in rotation order (CSV of [Position] names); excluded ones are absent. */
    val rotation: String,
    val currentPosition: Position,
)

/** One slot of a plan = one turn event. */
@Entity(
    tableName = "turn_slot",
    foreignKeys = [
        ForeignKey(entity = TurnPlan::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("planId"), Index("status")],
)
data class TurnSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val slotAt: Long,
    val firedAt: Long? = null,
    val ackAt: Long? = null,
    val positionFrom: Position,
    val positionTo: Position? = null,
    val status: TurnSlotStatus = TurnSlotStatus.PENDING,
    val skipReason: SkipReason? = null,
    val snoozeCount: Int = 0,
    val snoozeUntil: Long? = null,
    /** "Start round" tapped: no more sound for this slot (it stays open until done or missed). */
    val silenced: Boolean = false,
    /** "Remind me in 30 min". */
    val remindAt: Long? = null,
)

data class TurnPlanWithSlots(
    @Embedded val plan: TurnPlan,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val slots: List<TurnSlot>,
)

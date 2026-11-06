package app.transfusionvs.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * What it takes to take one action back: "Undo" for 5 seconds and "Undo last" on the bed's card for
 * 5 minutes. [snapshot] holds the rows as they were (see UndoCodec). Status and times only, no clinical data.
 */
@Entity(tableName = "undo_action", indices = [Index("ownerKey")])
data class UndoAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "T12" = transfusion 12, "P3" = turning plan 3, [ROUND] = a whole round at once. */
    val ownerKey: String,
    /** "7654814 · 15-min check done". Masked wherever it shows over the lock screen. */
    val label: String,
    val at: Long,
    val expiresAt: Long,
    val snapshot: String,
) {
    companion object {
        const val ROUND = "ROUND"
        fun transfusion(id: Long) = "T$id"
        fun plan(id: Long) = "P$id"
        fun custom(id: Long) = "C$id"
    }
}

@Dao
interface WardDao {

    // ---- Undo ----

    @Insert
    suspend fun insertUndo(action: UndoAction): Long

    @Query("SELECT * FROM undo_action WHERE id = :id")
    suspend fun undo(id: Long): UndoAction?

    /** Newest first. */
    @Query("SELECT * FROM undo_action ORDER BY at DESC, id DESC")
    fun observeUndo(): Flow<List<UndoAction>>

    @Query("SELECT * FROM undo_action ORDER BY at DESC, id DESC")
    suspend fun undoAll(): List<UndoAction>

    @Query("DELETE FROM undo_action WHERE id = :id")
    suspend fun deleteUndo(id: Long)

    /** A whole round and its per-bed entries share one time. */
    @Query("DELETE FROM undo_action WHERE at = :at AND ownerKey = :ownerKey")
    suspend fun deleteUndoAt(at: Long, ownerKey: String)

    @Query("DELETE FROM undo_action WHERE at = :at")
    suspend fun deleteAllUndoAt(at: Long)

    @Query("DELETE FROM undo_action WHERE expiresAt <= :now")
    suspend fun deleteExpiredUndo(now: Long)

    // ---- Custom timers ----

    @Insert
    suspend fun insertCustomTimer(t: CustomTimer): Long

    @Update
    suspend fun updateCustomTimer(t: CustomTimer)

    @Query("SELECT * FROM custom_timer WHERE id = :id")
    suspend fun customTimer(id: Long): CustomTimer?

    @Query("SELECT * FROM custom_timer")
    suspend fun allCustomTimers(): List<CustomTimer>

    @Query("SELECT * FROM custom_timer WHERE status = 'RUNNING' AND bedLabel = :mrn")
    suspend fun runningCustomFor(mrn: String): List<CustomTimer>

    /** Undoing a start. Its alarms go too (CASCADE). */
    @Query("DELETE FROM custom_timer WHERE id = :id")
    suspend fun deleteCustomTimer(id: Long)

    @Insert
    suspend fun insertCustomSlots(rows: List<CustomSlot>)

    @Update
    suspend fun updateCustomSlots(rows: List<CustomSlot>)

    @Query("DELETE FROM custom_slot WHERE id IN (:ids)")
    suspend fun deleteCustomSlots(ids: List<Long>)

    @Query("SELECT * FROM custom_slot WHERE timerId = :timerId ORDER BY slotAt")
    suspend fun customSlotsFor(timerId: Long): List<CustomSlot>

    @Query("SELECT * FROM custom_slot WHERE id IN (:ids)")
    suspend fun customSlots(ids: List<Long>): List<CustomSlot>

    @Query("SELECT * FROM custom_slot WHERE status = 'RINGING'")
    suspend fun ringingCustomSlots(): List<CustomSlot>

    /** Home: running timers only (a stopped one leaves Home at once). */
    @Transaction
    @Query("SELECT * FROM custom_timer WHERE status = 'RUNNING' ORDER BY startAt")
    fun observeCustomHome(): Flow<List<CustomTimerWithSlots>>

    /** Once, for the home-screen widget. */
    @Transaction
    @Query("SELECT * FROM custom_timer WHERE status = 'RUNNING' ORDER BY startAt")
    suspend fun customHomeOnce(): List<CustomTimerWithSlots>

    @Transaction
    @Query("SELECT * FROM custom_timer WHERE id = :id")
    fun observeCustomTimer(id: Long): Flow<CustomTimerWithSlots?>

    /** Timers with an alarm open (for the alarm screen), oldest start first. */
    @Transaction
    @Query("SELECT * FROM custom_timer WHERE id IN (SELECT timerId FROM custom_slot WHERE status = 'RINGING') ORDER BY startAt")
    fun observeCustomRinging(): Flow<List<CustomTimerWithSlots>>

    @Transaction
    @Query("SELECT * FROM custom_timer WHERE id IN (SELECT timerId FROM custom_slot WHERE status = 'RINGING') ORDER BY startAt")
    suspend fun customRinging(): List<CustomTimerWithSlots>

    @Query("SELECT bedLabel FROM custom_timer")
    suspend fun customBedLabels(): List<String>

    @Query("SELECT bedLabel FROM custom_timer")
    fun observeCustomBedLabels(): Flow<List<String>>

    // ---- Housekeeping: nothing is kept long-term (Cerner is the record) ----

    @Query(
        """
        DELETE FROM custom_timer WHERE status = 'STOPPED' AND stoppedAt IS NOT NULL AND stoppedAt < :cutoff
          AND id NOT IN (SELECT timerId FROM custom_slot WHERE status = 'RINGING')
        """,
    )
    suspend fun deleteStoppedCustomBefore(cutoff: Long)

    @Query("DELETE FROM custom_slot WHERE status IN ('ACKED', 'MISSED', 'CANCELLED') AND slotAt < :cutoff")
    suspend fun deleteOldCustomSlots(cutoff: Long)
}

package app.transfusionvs.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A patient this phone has seen lately, so the next plan for them takes one tap instead of typing.
 * The MRN only, never a name. It goes a day after its last use, once nothing is running for it.
 */
@Entity(tableName = "patient")
data class Patient(@PrimaryKey val mrn: String, val lastUsedAt: Long)

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
    }
}

@Dao
interface WardDao {

    // ---- Recent patients ----

    @Query("SELECT * FROM patient")
    fun observePatients(): Flow<List<Patient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun usePatient(patient: Patient)

    @Query("DELETE FROM patient WHERE mrn = :mrn")
    suspend fun removePatient(mrn: String)

    @Query(
        """
        DELETE FROM patient WHERE lastUsedAt < :cutoff
          AND mrn NOT IN (SELECT bedLabel FROM transfusion)
          AND mrn NOT IN (SELECT bedLabel FROM turn_plan)
        """,
    )
    suspend fun forgetPatients(cutoff: Long)

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
}

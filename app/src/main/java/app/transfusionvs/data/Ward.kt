package app.transfusionvs.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One of the ward's beds, entered once in Settings. A bed number, never a patient identifier. */
@Entity(tableName = "ward_bed")
data class WardBed(@PrimaryKey val label: String)

/**
 * What it takes to take one action back: "Undo" for 5 seconds and "Undo last" on the bed's card for
 * 5 minutes. [snapshot] holds the rows as they were (see UndoCodec). Status and times only, no clinical data.
 */
@Entity(tableName = "undo_action", indices = [Index("ownerKey")])
data class UndoAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "T12" = transfusion 12, "P3" = turning plan 3, [ROUND] = a whole round at once. */
    val ownerKey: String,
    /** "Bed 14 · 15-min check done". */
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

    // ---- Ward beds ----

    @Query("SELECT * FROM ward_bed")
    fun observeBeds(): Flow<List<WardBed>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBeds(beds: List<WardBed>)

    @Query("DELETE FROM ward_bed WHERE label = :label")
    suspend fun removeBed(label: String)

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

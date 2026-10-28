package app.transfusionvs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val WITH_BED = """
    SELECT c.id, c.transfusionId, c.type, c.offsetMin, c.scheduledAt, c.firedAt, c.ackAt,
           c.snoozeCount, c.snoozeUntil, c.status,
           t.bedLabel, t.colorIndex, t.startAt, t.plannedDurationMin, t.isTest
    FROM checkpoint c JOIN transfusion t ON t.id = c.transfusionId
"""

@Dao
interface TvsDao {

    // ---- Transfusions ----

    @Insert
    suspend fun insertTransfusion(t: Transfusion): Long

    @Update
    suspend fun updateTransfusion(t: Transfusion)

    @Query("SELECT * FROM transfusion WHERE id = :id")
    suspend fun transfusion(id: Long): Transfusion?

    @Query("SELECT * FROM transfusion WHERE status = 'RUNNING'")
    suspend fun runningTransfusions(): List<Transfusion>

    @Query("SELECT colorIndex FROM transfusion WHERE status = 'RUNNING' AND isTest = 0")
    suspend fun runningColors(): List<Int>

    /**
     * Home: running transfusions, plus ended ones that still need something
     * (End VS not yet documented, a missed check not acknowledged, or something ringing).
     */
    @Transaction
    @Query(
        """
        SELECT * FROM transfusion WHERE isTest = 0 AND (
            status = 'RUNNING' OR id IN (
                SELECT transfusionId FROM checkpoint
                WHERE status = 'RINGING'
                   OR (status = 'MISSED' AND ackAt IS NULL)
                   OR (type = 'END' AND status = 'PENDING')
            )
        ) ORDER BY startAt
        """,
    )
    fun observeHome(): Flow<List<TransfusionWithCheckpoints>>

    @Transaction
    @Query("SELECT * FROM transfusion WHERE id = :id")
    fun observeTransfusion(id: Long): Flow<TransfusionWithCheckpoints?>

    // ---- Checkpoints ----

    @Insert
    suspend fun insertCheckpoints(rows: List<Checkpoint>)

    @Update
    suspend fun updateCheckpoints(rows: List<Checkpoint>)

    @Query("DELETE FROM checkpoint WHERE id IN (:ids)")
    suspend fun deleteCheckpoints(ids: List<Long>)

    @Query("SELECT * FROM checkpoint WHERE transfusionId = :transfusionId ORDER BY offsetMin")
    suspend fun checkpointsFor(transfusionId: Long): List<Checkpoint>

    @Query("SELECT * FROM checkpoint WHERE id IN (:ids)")
    suspend fun checkpoints(ids: List<Long>): List<Checkpoint>

    /** Everything the alarm engine still has to act on. */
    @Query("SELECT * FROM checkpoint WHERE status IN ('PENDING', 'RINGING')")
    suspend fun activeCheckpoints(): List<Checkpoint>

    @Query("SELECT * FROM checkpoint WHERE status = 'MISSED' AND ackAt IS NULL")
    suspend fun missedUnacknowledged(): List<Checkpoint>

    @Query("$WITH_BED WHERE c.status = 'RINGING' ORDER BY c.scheduledAt, t.bedLabel")
    suspend fun ringingWithBed(): List<CheckpointWithBed>

    @Query("$WITH_BED WHERE c.status = 'RINGING' ORDER BY c.scheduledAt, t.bedLabel")
    fun observeRinging(): Flow<List<CheckpointWithBed>>

    @Query("$WITH_BED WHERE c.id IN (:ids)")
    suspend fun withBed(ids: List<Long>): List<CheckpointWithBed>

    @Query("$WITH_BED WHERE c.type = 'TEST' ORDER BY c.scheduledAt DESC LIMIT 10")
    fun observeTests(): Flow<List<CheckpointWithBed>>
}

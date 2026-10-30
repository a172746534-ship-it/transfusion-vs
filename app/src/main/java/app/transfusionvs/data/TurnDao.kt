package app.transfusionvs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnDao {

    // ---- Plans ----

    @Insert
    suspend fun insertPlan(p: TurnPlan): Long

    @Update
    suspend fun updatePlan(p: TurnPlan)

    @Query("SELECT * FROM turn_plan WHERE id = :id")
    suspend fun plan(id: Long): TurnPlan?

    @Query("SELECT * FROM turn_plan")
    suspend fun allPlans(): List<TurnPlan>

    @Query("SELECT colorIndex FROM turn_plan WHERE status = 'RUNNING'")
    suspend fun runningColors(): List<Int>

    // ---- Slots (turn events) ----

    @Insert
    suspend fun insertSlots(rows: List<TurnSlot>)

    @Update
    suspend fun updateSlots(rows: List<TurnSlot>)

    @Query("DELETE FROM turn_slot WHERE id IN (:ids)")
    suspend fun deleteSlots(ids: List<Long>)

    @Query("SELECT * FROM turn_slot WHERE planId = :planId ORDER BY slotAt")
    suspend fun slotsFor(planId: Long): List<TurnSlot>

    @Query("SELECT * FROM turn_slot WHERE id IN (:ids)")
    suspend fun slots(ids: List<Long>): List<TurnSlot>

    @Query("SELECT * FROM turn_slot WHERE status = 'RINGING'")
    suspend fun ringingSlots(): List<TurnSlot>

    // ---- Screens ----

    /** Home: running plans first, then stopped ones (kept 24 h so they can be restarted). */
    @Transaction
    @Query("SELECT * FROM turn_plan ORDER BY status, bedLabel")
    fun observeHome(): Flow<List<TurnPlanWithSlots>>

    @Transaction
    @Query("SELECT * FROM turn_plan WHERE id = :id")
    fun observePlan(id: Long): Flow<TurnPlanWithSlots?>

    /** Plans with a slot ringing or open (for the round alarm). */
    @Transaction
    @Query("SELECT * FROM turn_plan WHERE id IN (SELECT planId FROM turn_slot WHERE status = 'RINGING') ORDER BY bedLabel")
    fun observeRound(): Flow<List<TurnPlanWithSlots>>

    @Transaction
    @Query("SELECT * FROM turn_plan WHERE id IN (SELECT planId FROM turn_slot WHERE status = 'RINGING') ORDER BY bedLabel")
    suspend fun round(): List<TurnPlanWithSlots>

    // ---- Housekeeping (the phone keeps nothing long-term; Cerner is the record) ----

    @Query(
        """
        DELETE FROM turn_plan WHERE status = 'STOPPED' AND stopAt IS NOT NULL AND stopAt < :cutoff
          AND id NOT IN (SELECT planId FROM turn_slot WHERE status = 'RINGING')
        """,
    )
    suspend fun deleteStoppedBefore(cutoff: Long)

    /** Old finished slots go, except each plan's latest turn (so "last turn" never goes blank). */
    @Query(
        """
        DELETE FROM turn_slot WHERE status IN ('ACKED', 'MISSED', 'SKIPPED', 'CANCELLED') AND slotAt < :cutoff
          AND id NOT IN (SELECT MAX(id) FROM turn_slot WHERE status = 'ACKED' GROUP BY planId)
        """,
    )
    suspend fun deleteOldSlots(cutoff: Long)
}

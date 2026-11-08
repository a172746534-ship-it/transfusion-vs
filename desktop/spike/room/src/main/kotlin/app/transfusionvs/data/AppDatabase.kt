package app.transfusionvs.data

import androidx.room.Database
import androidx.room.RoomDatabase

// Spike only: the reference's seven entities and three DAOs as a desktop version 1 (CHANGE_PLAN §3.9).
@Database(
    entities = [Transfusion::class, Checkpoint::class, TurnPlan::class, TurnSlot::class, UndoAction::class, CustomTimer::class, CustomSlot::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TvsDao
    abstract fun turnDao(): TurnDao
    abstract fun wardDao(): WardDao
}

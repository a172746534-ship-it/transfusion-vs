package app.transfusionvs.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * v1 = transfusions. v2 adds repositioning (turn_plan, turn_slot). v3 adds the ward's bed list and
 * undo (ward_bed, undo_action). Both are automatic migrations that only create new tables, so
 * running transfusions and their alarms survive the update (MigrationTest).
 */
@Database(
    entities = [Transfusion::class, Checkpoint::class, TurnPlan::class, TurnSlot::class, WardBed::class, UndoAction::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TvsDao
    abstract fun turnDao(): TurnDao
    abstract fun wardDao(): WardDao

    companion object {
        const val NAME = "transfusion_vs.db"

        /**
         * The database lives in device-protected storage so alarms can be re-armed right after
         * a reboot, before the phone is unlocked (Direct Boot). It holds no patient identifiers.
         */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.createDeviceProtectedStorageContext(),
                AppDatabase::class.java,
                NAME,
            ).build()
    }
}

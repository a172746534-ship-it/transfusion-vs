package app.transfusionvs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transfusion::class, Checkpoint::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TvsDao

    companion object {
        /**
         * The database lives in device-protected storage so alarms can be re-armed right after
         * a reboot, before the phone is unlocked (Direct Boot). It holds no patient identifiers.
         */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.createDeviceProtectedStorageContext(),
                AppDatabase::class.java,
                "transfusion_vs.db",
            ).build()
    }
}

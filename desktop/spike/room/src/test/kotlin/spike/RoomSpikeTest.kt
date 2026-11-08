package spike

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.transfusionvs.data.AppDatabase
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Transfusion
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSpikeTest {
    private fun roundTrip(db: AppDatabase) = runBlocking {
        val id = db.dao().insertTransfusion(
            Transfusion(bedLabel = "7654812", colorIndex = 0, startAt = 1_000_000L, plannedDurationMin = 120, baselineConfirmed = true),
        )
        db.dao().insertCheckpoints(listOf(Checkpoint(transfusionId = id, type = CheckpointType.M15, offsetMin = 15, scheduledAt = 1_900_000L)))
        val home = db.dao().observeHome().first()
        println("SPIKE room: home=${home.size} checkpoints=${home.single().checkpoints.size}")
        assertEquals(1, home.size)
        assertEquals("7654812", db.dao().transfusion(id)!!.bedLabel)
        db.close()
    }

    @Test
    fun fileDatabaseWithTheBundledDriver() {
        val f = File.createTempFile("tvs", ".db").also { it.delete() }
        roundTrip(
            Room.databaseBuilder<AppDatabase>(name = f.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build(),
        )
        println("SPIKE room: file ${f.length()} bytes; driver library path property=${System.getProperty("androidx.sqlite.driver.bundled.path")}")
    }

    @Test
    fun inMemoryDatabase() = roundTrip(
        Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build(),
    )
}

package app.transfusionvs

import android.app.Application
import android.content.Context
import app.transfusionvs.alarm.AlarmScheduler
import app.transfusionvs.alarm.Engine
import app.transfusionvs.alarm.Notifications
import app.transfusionvs.data.AppDatabase
import app.transfusionvs.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency wiring — no DI framework. Everything is created lazily and lives as long as the process. */
class App : Application() {

    /** For work that must outlive a screen or a broadcast (receivers use goAsync with it). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db by lazy { AppDatabase.open(this) }
    val settings by lazy { Settings(this) }
    val scheduler by lazy { AlarmScheduler(this) }
    val engine by lazy { Engine(this, db.dao(), db.turnDao(), settings, scheduler) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}

val Context.app: App get() = applicationContext as App

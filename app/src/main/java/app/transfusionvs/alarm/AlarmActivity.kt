package app.transfusionvs.alarm

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.app
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.Settings
import app.transfusionvs.schedule.Labels
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.MeasureDocumentBlocks
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The alarm screen, shown over the lock screen with the screen turned on and kept on.
 * It lists everything ringing (any bed) and closes itself once all of it is acknowledged.
 * Back is blocked and there is no swipe-to-dismiss.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent { TvsTheme { AlarmScreen(onAllDone = { finish() }) } }
    }

    override fun onResume() {
        super.onResume()
        // This screen is visible, so it may start the ringing service itself if it isn't running
        // (e.g. it was opened by the fallback notification).
        app.scope.launch { app.engine.syncRinging(canStartService = true) }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun AlarmScreen(onAllDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val rows by app.db.dao().observeRinging().collectAsStateWithLifecycle(initialValue = null)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(rows) { if (rows?.isEmpty() == true) onAllDone() }
    BackHandler { /* blocked: acknowledge or snooze */ }

    val list = rows ?: emptyList()
    val beds = RingSpec.groupByBed(list)
    val audible = list.filter { it.snoozeUntil == null || it.snoozeUntil <= now }
    val canSnooze = audible.isNotEmpty() && audible.all { it.snoozeCount < Settings.MAX_SNOOZES }
    val snoozedUntil = list.mapNotNull { it.snoozeUntil }.filter { it > now }.minOrNull()
    val isTest = beds.isNotEmpty() && beds.all { it.isTest }

    Box(Modifier.fillMaxSize().background(Tvs.Navy).safeDrawingPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CapsLabel(if (isTest) "Test alarm" else "Blood transfusion running", Tvs.Amber)
            Text(
                when {
                    beds.isEmpty() -> ""
                    beds.size == 1 -> beds[0].title
                    else -> "${beds.size} beds due now"
                },
                color = Tvs.White, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black,
            )
            if (beds.size == 1) {
                Text(beds[0].subtitle, color = Tvs.White.copy(alpha = 0.85f), fontSize = 17.sp)
            }

            MeasureDocumentBlocks()

            if (snoozedUntil != null) {
                Text(
                    "Snoozed — rings again at ${Labels.clock(snoozedUntil)}",
                    color = Tvs.Amber, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                )
            }

            if (beds.size == 1) {
                AckButton("Measured & documented ✓", big = true) {
                    app.scope.launch { app.engine.acknowledge(beds[0].checkpointIds) }
                }
            } else {
                beds.forEach { bed -> BedRow(bed, list) { app.scope.launch { app.engine.acknowledge(bed.checkpointIds) } } }
            }

            if (canSnooze) {
                val snoozeMin = app.settings.snoozeMin
                OutlinedButton(
                    onClick = { app.scope.launch { app.engine.snooze() } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Tvs.White),
                ) { Text("Snooze $snoozeMin min", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(8.dp))
            ReactionBanner()
        }
    }
}

@Composable
private fun BedRow(bed: RingBed, rows: List<CheckpointWithBed>, onAck: () -> Unit) {
    val due = rows.filter { it.id in bed.checkpointIds }.minOfOrNull { it.scheduledAt }
    Column(
        Modifier.fillMaxWidth().background(Tvs.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(Tvs.Amber, CircleShape))
            Spacer(Modifier.size(10.dp))
            Column {
                Text(bed.shortLabel, color = Tvs.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    bed.what + (due?.let { " · due ${Labels.clock(it)}" } ?: ""),
                    color = Tvs.White.copy(alpha = 0.85f), fontSize = 16.sp,
                )
            }
        }
        AckButton("✓ ${bed.shortLabel} — measured & documented", big = false, onClick = onAck)
    }
}

@Composable
private fun AckButton(text: String, big: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (big) 96.dp else 64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Tvs.Amber, contentColor = Tvs.Navy),
    ) {
        Text(text, fontSize = if (big) 24.sp else 18.sp, fontWeight = FontWeight.Black)
    }
}

/** The card's red "Suspected reaction?" bar. (The full reaction screen arrives in Phase 2.) */
@Composable
private fun ReactionBanner() {
    Column(
        Modifier.fillMaxWidth().background(Tvs.Red, RoundedCornerShape(16.dp)).padding(16.dp),
    ) {
        Text("⚠ SUSPECTED REACTION? STOP the transfusion & call", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text("Keep the IV line open · call the doctor now", color = Color.White, fontSize = 15.sp)
    }
}

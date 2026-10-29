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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import app.transfusionvs.ui.components.ReactionBanner
import app.transfusionvs.ui.screens.BedChoice
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.components.PositionChange
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.Position
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
    AlarmContent(
        rows = rows ?: emptyList(),
        now = now,
        snoozeMin = app.settings.snoozeMin,
        onAcknowledge = { ids -> app.scope.launch { app.engine.acknowledge(ids) } },
        onSnooze = { app.scope.launch { app.engine.snooze() } },
        onStopForReaction = { id -> app.scope.launch { app.engine.stopForReaction(id) } },
    )
}

/** Stateless alarm screen (rendered by the screenshot tests with fake data). */
@Composable
fun AlarmContent(
    rows: List<CheckpointWithBed>,
    now: Long,
    snoozeMin: Int,
    onAcknowledge: (List<Long>) -> Unit,
    onSnooze: () -> Unit,
    onStopForReaction: (Long) -> Unit,
    round: RoundUi? = null,
    onStartRound: () -> Unit = {},
    onTurned: (slotId: Long, Position) -> Unit = { _, _ -> },
    onCantTurn: (slotId: Long, SkipReason) -> Unit = { _, _ -> },
    onSnoozeRound: () -> Unit = {},
    onRemind: () -> Unit = {},
    /** For screenshots: which beds start with "Other position" / "Can't turn" open. */
    initiallyOpen: Map<Long, RoundChoice> = emptyMap(),
) {
    var showReaction by rememberSaveable { mutableStateOf(false) }
    // Back never dismisses the alarm (acknowledge or snooze); it only closes the reaction view.
    BackHandler { showReaction = false }

    val list = rows
    val beds = RingSpec.groupByBed(list)
    val audible = list.filter { it.snoozeUntil == null || it.snoozeUntil <= now }
    val canSnooze = audible.isNotEmpty() && audible.all { it.snoozeCount < Settings.MAX_SNOOZES }
    val snoozedUntil = list.mapNotNull { it.snoozeUntil }.filter { it > now }.minOrNull()
    val isTest = beds.isNotEmpty() && beds.all { it.isTest }

    if (showReaction) {
        // Works over the lock screen too: no unlock needed in an emergency.
        ReactionScreen(
            beds = beds.filter { !it.isTest }.map { BedChoice(it.transfusionId, it.bedLabel, it.colorIndex) },
            onStopped = { id ->
                onStopForReaction(id)
                showReaction = false
            },
            onBack = { showReaction = false },
            backLabel = "‹  Back to alarm",
        )
        return
    }

    Box(Modifier.fillMaxSize().background(Tvs.Navy).safeDrawingPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TvsSpace.s20),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            if (beds.isNotEmpty()) {
                TransfusionSection(beds, list, isTest, canSnooze, snoozedUntil, snoozeMin, onAcknowledge, onSnooze)
            }
            // Turning round below any transfusion due in the same minute (transfusion first).
            if (round != null && round.beds.isNotEmpty()) {
                if (beds.isNotEmpty()) Spacer(Modifier.height(TvsSpace.s8))
                RoundSection(
                    round, big = beds.isEmpty(), snoozeMin = snoozeMin, initiallyOpen = initiallyOpen,
                    onStartRound = onStartRound, onTurned = onTurned, onCantTurn = onCantTurn,
                    onSnoozeRound = onSnoozeRound, onRemind = onRemind,
                )
            }
            if (beds.isNotEmpty()) {
                Spacer(Modifier.height(TvsSpace.s8))
                ReactionBanner(onClick = { showReaction = true })
            }
        }
    }
}

@Composable
private fun TransfusionSection(
    beds: List<RingBed>,
    list: List<CheckpointWithBed>,
    isTest: Boolean,
    canSnooze: Boolean,
    snoozedUntil: Long?,
    snoozeMin: Int,
    onAcknowledge: (List<Long>) -> Unit,
    onSnooze: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s16)) {
            CapsLabel(if (isTest) "Test alarm" else "Blood transfusion running", Tvs.Amber)
            Text(
                when {
                    beds.isEmpty() -> ""
                    beds.size == 1 -> beds[0].title
                    else -> "${beds.size} beds due now"
                },
                color = Tvs.White, fontSize = TvsFont.AlarmTitle, lineHeight = TvsFont.AlarmTitleLine, fontWeight = FontWeight.Black,
            )
            if (beds.size == 1) {
                Text(beds[0].subtitle, color = Tvs.OnNavyMuted, fontSize = TvsFont.BodyLarge)
            }

            MeasureDocumentBlocks()

            if (snoozedUntil != null) {
                Text(
                    "Snoozed — rings again at ${Labels.clock(snoozedUntil)}",
                    color = Tvs.Amber, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Bold,
                )
            }

            if (beds.size == 1) {
                AckButton("Measured & documented ✓", big = true) { onAcknowledge(beds[0].checkpointIds) }
            } else {
                beds.forEach { bed -> BedRow(bed, list) { onAcknowledge(bed.checkpointIds) } }
            }

            if (canSnooze) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                    shape = TvsShape.Card,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Tvs.White),
                ) { Text("Snooze $snoozeMin min", fontSize = TvsFont.Lead, fontWeight = FontWeight.Bold) }
            }
    }
}

@Composable
private fun BedRow(bed: RingBed, rows: List<CheckpointWithBed>, onAck: () -> Unit) {
    val due = rows.filter { it.id in bed.checkpointIds }.minOfOrNull { it.scheduledAt }
    Column(
        Modifier.fillMaxWidth().background(Tvs.OnNavyTint, TvsShape.Card).padding(TvsSpace.s14),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s10),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(TvsSize.Dot).background(Tvs.bedColor(bed.colorIndex), CircleShape))
            Spacer(Modifier.size(TvsSpace.s10))
            Column {
                Text(bed.shortLabel, color = Tvs.White, fontSize = TvsFont.Number, fontWeight = FontWeight.Black)
                Text(
                    bed.what + (due?.let { " · due ${Labels.clock(it)}" } ?: ""),
                    color = Tvs.OnNavyMuted, fontSize = TvsFont.Body,
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
        modifier = Modifier.fillMaxWidth().heightIn(min = if (big) TvsSize.AckHuge else TvsSize.TouchLarge),
        shape = TvsShape.Card,
        colors = ButtonDefaults.buttonColors(containerColor = Tvs.Amber, contentColor = Tvs.Navy),
    ) {
        Text(text, fontSize = if (big) TvsFont.Title else TvsFont.Lead, fontWeight = FontWeight.Black)
    }
}

/** "Turning round · 10:00 · 4 beds": one alarm for every bed due on this slot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundSection(
    round: RoundUi,
    big: Boolean,
    snoozeMin: Int,
    initiallyOpen: Map<Long, RoundChoice>,
    onStartRound: () -> Unit,
    onTurned: (Long, Position) -> Unit,
    onCantTurn: (Long, SkipReason) -> Unit,
    onSnoozeRound: () -> Unit,
    onRemind: () -> Unit,
) {
    val open = remember { mutableStateMapOf<Long, RoundChoice>().apply { putAll(initiallyOpen) } }
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s16)) {
        CapsLabel("Repositioning round", Tvs.Amber)
        Text(
            "Turning round · ${Labels.clock(round.slotAt)} · ${round.beds.size} bed${if (round.beds.size > 1) "s" else ""}",
            color = Tvs.White,
            fontSize = if (big) TvsFont.AlarmTitle else TvsFont.Title,
            lineHeight = if (big) TvsFont.AlarmTitleLine else TvsFont.TitleLine,
            fontWeight = FontWeight.Black,
        )
        Text("Check pressure points: occiput · ears · sacrum · heels", color = Tvs.OnNavyMuted, fontSize = TvsFont.BodyLarge)

        if (round.snoozedUntil != null) {
            Text(
                "Snoozed — rings again at ${Labels.clock(round.snoozedUntil)}",
                color = Tvs.Amber, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Bold,
            )
        }

        if (!round.started) {
            // Before "Start round": who and what, then one big button that stops the sound.
            round.beds.forEach { bed ->
                // Same layout and sizes as a transfusion bed row: bed on one line, the turn below.
                Column(
                    Modifier.fillMaxWidth().background(Tvs.OnNavyTint, TvsShape.Card).padding(TvsSpace.s14),
                    verticalArrangement = Arrangement.spacedBy(TvsSpace.s6),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(TvsSize.Dot).background(Tvs.bedColor(bed.colorIndex), CircleShape))
                        Spacer(Modifier.size(TvsSpace.s10))
                        Text(bed.bedLabel, color = Tvs.White, fontSize = TvsFont.Number, fontWeight = FontWeight.Black)
                    }
                    PositionChange(bed.from, bed.suggested, Tvs.White)
                }
            }
            AckButton("Start round", big = true, onClick = onStartRound)
            if (round.canSnooze && big) { // next to a transfusion, its Snooze above covers everything
                OutlinedButton(
                    onClick = onSnoozeRound,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                    shape = TvsShape.Card,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Tvs.White),
                ) { Text("Snooze $snoozeMin min", fontSize = TvsFont.Lead, fontWeight = FontWeight.Bold) }
            }
        } else {
            // The checklist: one row per bed.
            round.beds.forEach { bed ->
                RoundBedRow(
                    bed = bed,
                    open = open[bed.slotId],
                    onOpen = { choice -> if (open[bed.slotId] == choice) open.remove(bed.slotId) else open[bed.slotId] = choice },
                    onTurned = { p -> onTurned(bed.slotId, p) },
                    onCantTurn = { r -> onCantTurn(bed.slotId, r) },
                )
            }
            if (round.canRemind) {
                OutlinedButton(
                    onClick = onRemind,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                    shape = TvsShape.Card,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Tvs.White),
                ) { Text("Remind me in 30 min", fontSize = TvsFont.Lead, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Mirrors the transfusion bed row: bed, the turn to make, one big ✓, and two secondary actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundBedRow(
    bed: RoundBed,
    open: RoundChoice?,
    onOpen: (RoundChoice) -> Unit,
    onTurned: (Position) -> Unit,
    onCantTurn: (SkipReason) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Tvs.OnNavyTint, TvsShape.Card).padding(TvsSpace.s14),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s10),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(TvsSize.Dot).background(Tvs.bedColor(bed.colorIndex), CircleShape))
            Spacer(Modifier.size(TvsSpace.s10))
            Text(bed.bedLabel, color = Tvs.White, fontSize = TvsFont.Number, fontWeight = FontWeight.Black)
        }
        PositionChange(bed.from, bed.suggested, Tvs.White)
        AckButton("✓ Turned to ${bed.suggested.label}", big = false, onClick = { onTurned(bed.suggested) })
        Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            listOf(RoundChoice.OTHER_POSITION to "Other position", RoundChoice.CANT_TURN to "Can't turn").forEach { (choice, label) ->
                OutlinedButton(
                    onClick = { onOpen(choice) },
                    modifier = Modifier.weight(1f).heightIn(min = TvsSize.Touch),
                    contentPadding = PaddingValues(horizontal = TvsSpace.s8),
                    shape = TvsShape.Control,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (open == choice) Tvs.White else Tvs.Navy,
                        contentColor = if (open == choice) Tvs.Navy else Tvs.White,
                    ),
                ) { Text(label, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold) }
            }
        }
        when (open) {
            RoundChoice.OTHER_POSITION -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
            ) {
                // Neither the suggestion (that is the big button) nor where the patient already lies.
                bed.allowed.filter { it != bed.suggested && it != bed.from }.forEach { p -> ChoiceChip(p.label, false, { onTurned(p) }) }
            }
            RoundChoice.CANT_TURN -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
            ) {
                SkipReason.entries.forEach { r -> ChoiceChip(r.label, false, { onCantTurn(r) }) }
            }
            null -> Unit
        }
    }
}

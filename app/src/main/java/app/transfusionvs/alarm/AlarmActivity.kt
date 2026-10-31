package app.transfusionvs.alarm

import android.app.KeyguardManager
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.transfusionvs.R
import app.transfusionvs.app
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.Position
import app.transfusionvs.data.Settings
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.UndoAction
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.BedMask
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.schedule.Urgency
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.MeasureDocumentBlocks
import app.transfusionvs.ui.components.PositionChange
import app.transfusionvs.ui.components.Rail
import app.transfusionvs.ui.components.ReactionBar
import app.transfusionvs.ui.components.UNDO_BAR_MS
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.nightNow
import app.transfusionvs.ui.components.rememberNight
import app.transfusionvs.ui.screens.BedChoice
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsTheme
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The alarm screen, shown over the lock screen with the screen turned on and kept on.
 * It lists everything ringing (any bed) and closes itself once all of it is acknowledged.
 * Back is blocked and there is no swipe-to-dismiss. One tap must never be an accident: see [PocketGuard].
 */
class AlarmActivity : ComponentActivity(), SensorEventListener {

    private val guard = PocketGuard()
    private var proximity: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        // The window is already dark from the first frame; settings load off the main thread.
        lifecycleScope.launch {
            val settings = withContext(Dispatchers.IO) { app.settings.also { it.setupSeen } }
            if (nightNow(settings, resources.configuration)) {
                // At night the alarm must not light up a sleeping child's room more than it has to.
                window.attributes = window.attributes.apply { screenBrightness = NIGHT_BRIGHTNESS }
            }
            setContent {
                TvsTheme(night = rememberNight(app)) { AlarmScreen(onAllDone = { finish() }) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        guard.onShown(SystemClock.uptimeMillis())
        val sensors = getSystemService(SensorManager::class.java)
        proximity = sensors?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximity?.let { sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        // This screen is visible, so it may start the ringing service itself if it isn't running
        // (e.g. it was opened by the fallback notification).
        app.scope.launch { app.engine.syncRinging(canStartService = true) }
    }

    override fun onPause() {
        getSystemService(SensorManager::class.java)?.unregisterListener(this)
        guard.covered = false
        super.onPause()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (guard.allows(ev.actionMasked, ev.eventTime)) return super.dispatchTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) Log.i("PocketGuard", if (guard.covered) "touch ignored: covered" else "touch ignored: first second")
        return true
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensor = proximity ?: return
        guard.covered = event.values.firstOrNull()?.let { it < sensor.maximumRange } ?: false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

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

    private companion object {
        const val NIGHT_BRIGHTNESS = 0.2f
    }
}

@Composable
private fun AlarmScreen(onAllDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val rows by app.db.dao().observeRinging().collectAsStateWithLifecycle(initialValue = null)
    val plans by app.db.turnDao().observeRound().collectAsStateWithLifecycle(initialValue = null)
    val undo by app.db.wardDao().observeUndo().collectAsStateWithLifecycle(initialValue = null)
    val txLabels by app.db.dao().observeBedLabels().collectAsStateWithLifecycle(initialValue = emptyList())
    val planLabels by app.db.turnDao().observeBedLabels().collectAsStateWithLifecycle(initialValue = emptyList())

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    // Over the lock screen a long number in a bed label is masked; the whole of it needs an unlocked phone.
    val locked = remember(now) { context.getSystemService(KeyguardManager::class.java).isKeyguardLocked }
    val mask: (String) -> String = if (locked) BedMask.masker(txLabels + planLabels) else { label -> label }
    val nothingOpen = rows?.isEmpty() == true && plans?.isEmpty() == true
    val justDone = undo?.firstOrNull()?.takeIf { System.currentTimeMillis() - it.at < UNDO_BAR_MS }
    // Closes itself once nothing is open, after giving the last action its moment for Undo.
    LaunchedEffect(nothingOpen, undo == null, justDone?.id) {
        if (nothingOpen && undo != null) {
            if (justDone != null) delay(UNDO_BAR_MS - (System.currentTimeMillis() - justDone.at))
            onAllDone()
        }
    }
    if (nothingOpen) {
        DoneContent(justDone?.let { it.copy(label = mask(it.label)) }, onUndo = { id -> app.scope.launch { app.engine.undo(id) } })
        return
    }
    AlarmContent(
        rows = rows.orEmpty().map { it.copy(bedLabel = mask(it.bedLabel)) },
        now = now,
        snoozeMin = app.settings.snoozeMin,
        onAcknowledge = { ids -> app.scope.launch { app.engine.acknowledge(ids) } },
        onSnooze = { app.scope.launch { app.engine.snooze() } },
        onStopForReaction = { id -> app.scope.launch { app.engine.stopForReaction(id) } },
        round = RoundBuilder.build(plans.orEmpty(), now, ZoneId.systemDefault())?.masked(mask),
        onStartRound = { app.scope.launch { app.engine.startRound() } },
        onAllTurned = { except -> app.scope.launch { app.engine.allTurned(except) } },
        onTurned = { slotId, position -> app.scope.launch { app.engine.turned(slotId, position) } },
        onCantTurn = { slotId, reason -> app.scope.launch { app.engine.cantTurn(slotId, reason) } },
        onSnoozeRound = { app.scope.launch { app.engine.snooze() } },
        onRemind = { app.scope.launch { app.engine.remindTurns() } },
    )
}

/** All done: the alarm stays for its last 5 seconds with one big Undo, then closes itself. */
@Composable
fun DoneContent(done: UndoAction?, onUndo: (Long) -> Unit) {
    val c = Ward.c
    Column(
        Modifier.fillMaxSize().background(c.alarmBg).safeDrawingPadding().padding(TvsSpace.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
    ) {
        Spacer(Modifier.weight(1f))
        if (done != null) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.alarmAction, modifier = Modifier.size(TvsSize.AckHuge))
            Text(done.label, color = c.alarmInk, style = WardType.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            AlarmSecondary("Undo", { onUndo(done.id) })
        }
    }
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
    /** All open beds except the given slots (a bed with "Can't turn" or "Other position" open). */
    onAllTurned: (Set<Long>) -> Unit = {},
    onTurned: (slotId: Long, Position) -> Unit = { _, _ -> },
    onCantTurn: (slotId: Long, SkipReason) -> Unit = { _, _ -> },
    onSnoozeRound: () -> Unit = {},
    onRemind: () -> Unit = {},
    /** For screenshots: which beds start with "Other position" / "Can't turn" open. */
    initiallyOpen: Map<Long, RoundChoice> = emptyMap(),
) {
    val c = Ward.c
    var showReaction by rememberSaveable { mutableStateOf(false) }
    var roundFirst by rememberSaveable { mutableStateOf(false) }
    // Back never dismisses the alarm (acknowledge or snooze); it only closes the reaction view.
    BackHandler { showReaction = false }

    val beds = RingSpec.groupByBed(rows)
    val audible = rows.filter { it.snoozeUntil == null || it.snoozeUntil <= now }
    val canSnooze = audible.isNotEmpty() && audible.all { it.snoozeCount < Settings.MAX_SNOOZES }
    val snoozedUntil = rows.mapNotNull { it.snoozeUntil }.filter { it > now }.minOrNull()
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
        )
        return
    }

    Column(Modifier.fillMaxSize().background(c.alarmBg).safeDrawingPadding().padding(TvsSpace.s16)) {
        when {
            // A transfusion goes first; a round due in the same minute takes over once it is done, or
            // right away from its own button if that is what the nurse is doing.
            beds.isNotEmpty() && !(roundFirst && round != null) -> TransfusionAlarm(
                beds, rows, isTest, canSnooze, snoozedUntil, snoozeMin, round, onAcknowledge, onSnooze,
                onReaction = { showReaction = true },
                onStartRound = {
                    onStartRound()
                    roundFirst = true
                },
                now = now,
            )
            round != null && round.beds.isNotEmpty() -> RoundAlarm(
                round, snoozeMin, initiallyOpen, onStartRound, onAllTurned, onTurned, onCantTurn, onSnoozeRound, onRemind,
                stillRinging = beds.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.title },
                onBackToTransfusion = { roundFirst = false },
            )
        }
    }
}

@Composable
private fun AlarmPrimary(text: String, onClick: () -> Unit, big: Boolean = true) {
    val c = Ward.c
    WardButton(
        text, onClick, Modifier.fillMaxWidth().testTag(PRIMARY_TAG),
        fill = c.alarmAction, content = c.alarmOnAction,
        minHeight = if (big) TvsSize.AckHuge else TvsSize.TouchLarge,
        style = if (big) WardType.ButtonLarge else WardType.Button,
    )
}

@Composable
private fun AlarmSecondary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), style: TextStyle = WardType.Button) {
    val c = Ward.c
    WardButton(text, onClick, modifier, fill = c.alarmBg, content = c.alarmInk, edge = c.alarmMuted, style = style)
}

/** The Snooze slot never vanishes, so a thumb that learned its place can't land on something else. */
@Composable
private fun SnoozeSlot(canSnooze: Boolean, snoozedUntil: Long?, snoozeMin: Int, onSnooze: () -> Unit) {
    val c = Ward.c
    if (canSnooze) {
        AlarmSecondary("Snooze $snoozeMin min", onSnooze)
    } else {
        Row(
            Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge).padding(horizontal = TvsSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (snoozedUntil != null) {
                Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = c.alarmInk, modifier = Modifier.size(TvsSize.IconSmall))
                Spacer(Modifier.width(TvsSpace.s8))
            }
            Text(
                if (snoozedUntil != null) "Snoozed — rings again at ${Labels.clock(snoozedUntil)}" else "No snoozes left",
                color = c.alarmInk, style = WardType.Lead, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AlarmCaps(icon: Int, text: String) {
    val c = Ward.c
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = c.alarmCaps, modifier = Modifier.size(TvsSize.IconSmall))
        Spacer(Modifier.width(TvsSpace.s8))
        CapsLabel(text, c.alarmCaps)
    }
}

/** Bed and time side by side, big enough to read at arm's length; stacked when they can't fit. */
@Composable
private fun KeyNumbers(first: String, second: String, firstStyle: TextStyle = WardType.AlarmKey, secondStyle: TextStyle = WardType.AlarmKey) {
    val c = Ward.c
    if (first.length > 6 || largeFont()) {
        Text(first, color = c.alarmInk, style = firstStyle)
        Text(second, color = c.alarmInk, style = secondStyle)
    } else {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(first, color = c.alarmInk, style = firstStyle, modifier = Modifier.weight(1f))
            Text(second, color = c.alarmInk, style = secondStyle)
        }
    }
}

/** How urgent, on the screen itself: the sound isn't the only thing that says so. */
@Composable
private fun AlarmState(due: Long, now: Long, snoozedUntil: Long?) {
    val c = Ward.c
    val late = (now - due) / MINUTE_MS
    val shape = TvsShape.Pill
    if (snoozedUntil != null) {
        Box(Modifier.border(TvsSize.Edge, c.alarmInk, shape).padding(horizontal = TvsSpace.s8)) {
            Text("SNOOZED · ${Labels.clock(snoozedUntil)}", color = c.alarmInk, style = WardType.BodyBold)
        }
    } else {
        Box(Modifier.background(c.alarmAction, shape).padding(horizontal = TvsSpace.s8)) {
            Text(if (late >= 1) "DUE +$late min" else "DUE NOW", color = c.alarmOnAction, style = WardType.BodyBold)
        }
    }
}

@Composable
private fun AlarmSteps() {
    val c = Ward.c
    MeasureDocumentBlocks(c.alarmInk, c.alarmEdge, c.alarmStep, c.alarmBg)
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.TransfusionAlarm(
    beds: List<RingBed>,
    rows: List<CheckpointWithBed>,
    isTest: Boolean,
    canSnooze: Boolean,
    snoozedUntil: Long?,
    snoozeMin: Int,
    round: RoundUi?,
    onAcknowledge: (List<Long>) -> Unit,
    onSnooze: () -> Unit,
    onReaction: () -> Unit,
    onStartRound: () -> Unit,
    now: Long,
) {
    val c = Ward.c
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = if (beds.size > 1) Arrangement.spacedBy(TvsSpace.s12, Alignment.Bottom) else Arrangement.spacedBy(TvsSpace.s12),
    ) {
        AlarmCaps(R.drawable.ic_drop, if (isTest) "Test alarm" else "Blood transfusion running")
        val due = rows.minOfOrNull { it.scheduledAt }
        if (due != null) AlarmState(due, now, snoozedUntil)
        if (beds.size == 1) {
            val bed = beds[0]
            Text(bed.what, color = c.alarmInk, style = WardType.AlarmTitle)
            val due = rows.filter { it.id in bed.checkpointIds }.minOfOrNull { it.scheduledAt }
            KeyNumbers(bed.shortLabel, due?.let { Labels.clock(it) } ?: "")
            val first = rows.firstOrNull { it.id in bed.checkpointIds }
            if (!isTest && first != null) Rail(BedViews.alarmRail(first.offsetMin, first.plannedDurationMin), Urgency.NOW, height = TvsSize.RailLarge, ink = c.alarmInk, background = c.alarmBg, beadColor = c.alarmAction, onBead = c.alarmOnAction)
            if (bed.subtitle != Labels.subtitle(CheckpointType.M15)) Text(bed.subtitle, color = c.alarmInk, style = WardType.Lead)
        } else {
            Text("${beds.size} beds due now", color = c.alarmInk, style = WardType.AlarmTitle)
        }
        AlarmSteps()
        if (beds.size > 1) beds.forEach { bed -> BedRow(bed, rows) { onAcknowledge(bed.checkpointIds) } }
    }
    Spacer(Modifier.size(TvsSpace.s8))
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s12)) {
        // A round due in the same minute is on this screen too, and can be started from here.
        if (round != null && round.beds.isNotEmpty()) {
            WardButton(
                "Start round · ${Labels.clock(round.slotAt)} · ${round.beds.joinToString(", ") { it.bedLabel }}", onStartRound, Modifier.fillMaxWidth(),
                fill = c.alarmTint, content = c.alarmInk, style = WardType.ButtonSmall, icon = R.drawable.ic_turn,
            )
        }
        if (!isTest) ReactionBar(onClick = onReaction)
        SnoozeSlot(canSnooze, snoozedUntil, snoozeMin, onSnooze)
        if (beds.size == 1) AlarmPrimary(Labels.ACK, { onAcknowledge(beds[0].checkpointIds) })
    }
}

@Composable
private fun BedRow(bed: RingBed, rows: List<CheckpointWithBed>, onAck: () -> Unit) {
    val c = Ward.c
    val due = rows.filter { it.id in bed.checkpointIds }.minOfOrNull { it.scheduledAt }
    Column(
        Modifier.fillMaxWidth().background(c.alarmTint, TvsShape.Card).padding(TvsSpace.s12),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
    ) {
        KeyNumbers(bed.shortLabel, due?.let { Labels.clock(it) } ?: "", WardType.AlarmKeySmall, WardType.AlarmKeySmall)
        Text(bed.what, color = c.alarmMuted, style = WardType.Lead)
        WardButton(
            "${bed.shortLabel} · ${Labels.ACK}", onAck, Modifier.fillMaxWidth(),
            fill = c.alarmAction, content = c.alarmOnAction, style = WardType.ButtonSmall,
        )
    }
}

/**
 * One alarm for every bed due on this slot. "Start round" stops the sound, then one tap finishes the
 * whole round as suggested; only exceptions need more.
 */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RoundAlarm(
    round: RoundUi,
    snoozeMin: Int,
    initiallyOpen: Map<Long, RoundChoice>,
    onStartRound: () -> Unit,
    onAllTurned: (Set<Long>) -> Unit,
    onTurned: (Long, Position) -> Unit,
    onCantTurn: (Long, SkipReason) -> Unit,
    onSnoozeRound: () -> Unit,
    onRemind: () -> Unit,
    stillRinging: String? = null,
    onBackToTransfusion: () -> Unit = {},
) {
    val c = Ward.c
    val open = remember { mutableStateMapOf<Long, RoundChoice>().apply { putAll(initiallyOpen) } }
    val count = "${round.beds.size} bed${if (round.beds.size > 1) "s" else ""}"
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s10),
    ) {
        AlarmCaps(R.drawable.ic_turn, "Repositioning round")
        if (stillRinging != null) AlarmSecondary("Still due: $stillRinging", onBackToTransfusion, style = WardType.BodyBold)
        if (!round.started) Text("Turning round", color = c.alarmInk, style = WardType.AlarmTitle)
        KeyNumbers(Labels.clock(round.slotAt), count, WardType.AlarmKey, WardType.AlarmTitle)
        Text("Check pressure points: occiput · ears · sacrum · heels", color = c.alarmMuted, style = WardType.Body)
        round.beds.forEach { bed ->
            if (round.started) {
                RoundBedRow(
                    bed = bed,
                    open = open[bed.slotId],
                    onOpen = { choice -> if (open[bed.slotId] == choice) open.remove(bed.slotId) else open[bed.slotId] = choice },
                    onTurned = { p -> onTurned(bed.slotId, p) },
                    onCantTurn = { r -> onCantTurn(bed.slotId, r) },
                )
            } else {
                Column(
                    Modifier.fillMaxWidth().background(c.alarmTint, TvsShape.Control).padding(horizontal = TvsSpace.s12, vertical = TvsSpace.s8),
                    verticalArrangement = Arrangement.spacedBy(TvsSpace.s2),
                ) {
                    Text(bed.bedLabel, color = c.alarmInk, style = WardType.CardBed)
                    PositionChange(bed.from, bed.suggested, c.alarmInk)
                }
            }
        }
        // Down here, away from the primary action: a slip must not push the round out by 30 minutes.
        if (round.started && round.canRemind) AlarmSecondary("Remind me in 30 min", onRemind)
    }
    Spacer(Modifier.size(TvsSpace.s8))
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
        if (round.started) {
            val others = round.beds.count { it.slotId !in open.keys }
            when {
                open.isEmpty() -> AlarmPrimary("All turned as suggested\u00A0✓", { onAllTurned(emptySet()) })
                others > 0 -> AlarmPrimary("The other $others turned as suggested\u00A0✓", { onAllTurned(open.keys.toSet()) })
            }
        } else {
            SnoozeSlot(round.canSnooze, round.snoozedUntil, snoozeMin, onSnoozeRound)
            AlarmPrimary("Start round", onStartRound)
        }
    }
}

/** One bed on the started round: the turn to make, its own ✓, and the two exceptions. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun RoundBedRow(
    bed: RoundBed,
    open: RoundChoice?,
    onOpen: (RoundChoice) -> Unit,
    onTurned: (Position) -> Unit,
    onCantTurn: (SkipReason) -> Unit,
) {
    val c = Ward.c
    Column(
        Modifier.fillMaxWidth().background(c.alarmTint, TvsShape.Card).padding(TvsSpace.s12),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
    ) {
        Text(bed.bedLabel, color = c.alarmInk, style = WardType.CardBed)
        PositionChange(bed.from, bed.suggested, c.alarmInk)
        AlarmSecondary("Turned to ${bed.suggested.label}", { onTurned(bed.suggested) }, style = WardType.ButtonSmall)
        val stacked = largeFont()
        @Composable
        fun choice(choice: RoundChoice, label: String, modifier: Modifier) {
            // The open one is marked with a ✓ and a tint; the white chips below are what to tap next.
            val on = open == choice
            WardButton(
                if (on) "✓ $label" else label, { onOpen(choice) }, modifier,
                fill = if (on) c.alarmTint else c.alarmBg,
                content = c.alarmInk,
                edge = if (on) c.alarmInk else c.alarmMuted, style = WardType.BodyBold, shape = TvsShape.Control,
            )
        }
        if (stacked) {
            choice(RoundChoice.OTHER_POSITION, "Other position", Modifier.fillMaxWidth())
            choice(RoundChoice.CANT_TURN, "Can't turn", Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                choice(RoundChoice.OTHER_POSITION, "Other position", Modifier.weight(1f))
                choice(RoundChoice.CANT_TURN, "Can't turn", Modifier.weight(1f))
            }
        }
        // Scrolled into view by itself, above the pinned action: reaching the reason costs no touch.
        val reasons = remember { BringIntoViewRequester() }
        LaunchedEffect(open) {
            if (open != null) {
                withFrameNanos { } // once the reasons are laid out
                reasons.bringIntoView()
            }
        }
        when (open) {
            RoundChoice.OTHER_POSITION -> FlowRow(
                modifier = Modifier.bringIntoViewRequester(reasons),
                horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
            ) {
                // Neither the suggestion (that is the big button) nor where the patient already lies.
                bed.allowed.filter { it != bed.suggested && it != bed.from }.forEach { p ->
                    AlarmChip(p.label) { onTurned(p) }
                }
            }
            RoundChoice.CANT_TURN -> FlowRow(
                modifier = Modifier.bringIntoViewRequester(reasons),
                horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8),
                verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
            ) {
                SkipReason.entries.forEach { r -> AlarmChip(r.label) { onCantTurn(r) } }
            }
            null -> Unit
        }
    }
}

@Composable
private fun AlarmChip(text: String, onClick: () -> Unit) {
    val c = Ward.c
    WardButton(text, onClick, fill = c.alarmChip, content = c.alarmBg, style = WardType.BodyBold, shape = TvsShape.Control)
}

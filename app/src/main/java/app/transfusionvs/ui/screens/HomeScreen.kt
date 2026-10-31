package app.transfusionvs.ui.screens

import app.transfusionvs.schedule.Urgency
import androidx.compose.foundation.shape.CircleShape
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.UndoAction
import app.transfusionvs.schedule.BedType
import app.transfusionvs.schedule.BedView
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.schedule.Labels
import app.transfusionvs.setup.SetupChecks
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.PatientButton
import app.transfusionvs.ui.components.Alert
import app.transfusionvs.ui.components.BedCard
import app.transfusionvs.ui.components.EmptyCard
import app.transfusionvs.ui.components.ReactionBar
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.UndoSlot
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.moreBelow
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.components.wardClick
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onStartTransfusion: () -> Unit,
    onStartTurning: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenTurnPlan: (Long) -> Unit,
    onReaction: () -> Unit,
    onSetup: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val home by vm.home.collectAsStateWithLifecycle()
    val turns by vm.turns.collectAsStateWithLifecycle()
    val ringing by vm.ringing.collectAsStateWithLifecycle()
    val undo by vm.undo.collectAsStateWithLifecycle()
    val anyTransfusion by vm.anyTransfusion.collectAsStateWithLifecycle()
    val now by rememberNow(15_000)
    var setupProblems by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        setupProblems = SetupChecks.evaluate(context).count { !it.ok }
        onPauseOrDispose { }
    }
    val openAlarm = {
        context.startActivity(Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    HomeContent(
        home = home,
        turns = turns,
        anyTransfusion = anyTransfusion,
        undo = undo,
        missedTimeoutMin = vm.missedTimeoutMin,
        ringing = ringing.isNotEmpty(),
        setupProblems = setupProblems,
        now = now,
        onStartTransfusion = onStartTransfusion,
        onStartTurning = onStartTurning,
        onOpen = onOpen,
        onOpenTurnPlan = onOpenTurnPlan,
        onReaction = onReaction,
        onSetup = onSetup,
        onSettings = onSettings,
        onOpenAlarm = openAlarm,
        onAcknowledge = vm::acknowledge,
        onUndo = vm::undo,
    )
}

/**
 * Stateless Home (rendered by the screenshot tests with fake data). Open = done: every bed, most
 * urgent first, each with its next clock time. Quiet when all is well; when a bed needs something, its
 * action takes the one primary place, in the thumb zone.
 * [anyTransfusion]: the reaction row shows only when there is blood on the ward, running or finished
 * within the last day (a reaction can come late).
 */
@Composable
fun HomeContent(
    home: List<TransfusionWithCheckpoints>?,
    turns: List<TurnPlanWithSlots>?,
    anyTransfusion: Boolean = home.orEmpty().isNotEmpty(),
    undo: List<UndoAction> = emptyList(),
    missedTimeoutMin: Int = 10,
    ringing: Boolean,
    setupProblems: Int,
    now: Long,
    onStartTransfusion: () -> Unit,
    onStartTurning: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenTurnPlan: (Long) -> Unit,
    onReaction: () -> Unit,
    onSetup: () -> Unit,
    onSettings: () -> Unit,
    onOpenAlarm: () -> Unit,
    onAcknowledge: (List<Long>) -> Unit,
    onUndo: (Long) -> Unit = {},
) {
    val c = Ward.c
    val beds = BedViews.sorted(
        home.orEmpty().map { BedViews.of(it, now) } + turns.orEmpty().map { BedViews.of(it, now, missedTimeoutMin) },
    )
    val live = undo.filter { it.expiresAt > now }
    val large = largeFont()
    val urgent = beds.firstOrNull { (it.ackIds.isNotEmpty() && !it.stoppedForReaction) || it.roundOpen }
    val worstIsLate = beds.any { !it.idle && (it.stoppedForReaction || it.urgency == Urgency.OVERDUE) }
    val worstIsDue = beds.any { !it.idle && it.urgency == Urgency.NOW }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = BedViews.running(beds),
            title = (if (worstIsLate) "◆ " else "") + BedViews.summary(beds),
            trailing = {
                Spacer(Modifier.width(TvsSpace.s8))
                Text(Labels.clock(now), color = c.onHeader, style = WardType.CardTime, softWrap = false)
                Spacer(Modifier.width(TvsSpace.s8))
                Box(Modifier.size(TvsSize.Touch).background(c.headerTint, CircleShape).wardClick(onClick = onSettings), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings", tint = c.onHeader, modifier = Modifier.size(TvsSize.IconMedium))
                }
            },
        )

        // The first thing the eye lands on changes with the state, not only the cards below it.
        val strip = when {
            worstIsLate -> c.overdue
            worstIsDue -> c.alarmAction
            else -> null
        }
        if (strip != null) Box(Modifier.fillMaxWidth().height(TvsSpace.s6).background(strip))

        val list = rememberLazyListState()
        LazyColumn(
            Modifier.weight(1f).moreBelow(list.canScrollForward),
            state = list,
            contentPadding = PaddingValues(horizontal = TvsSpace.s12, vertical = TvsSpace.s4),
            // With nothing to list, the words sit just above the buttons they talk about.
            verticalArrangement = if (beds.isEmpty()) Arrangement.spacedBy(TvsSpace.s8, Alignment.Bottom) else Arrangement.spacedBy(TvsSpace.s8),
        ) {
            if (setupProblems > 0) {
                item {
                    Alert(
                        "Setup: $setupProblems to fix\nAlarms may not ring",
                        R.drawable.ic_warning, c.alertFill, c.onAlert, c.alertEdge, onSetup,
                    )
                }
            }
            items(beds, key = { "${it.type}${it.id}" }) { bed ->
                HomeBed(
                    bed = bed,
                    undo = live.firstOrNull { it.ownerKey == bed.ownerKey() },
                    onOpen = { if (bed.type == BedType.TRANSFUSION) onOpen(bed.id) else onOpenTurnPlan(bed.id) },
                    onUndo = onUndo,
                )
            }
            if (home != null && turns != null && beds.isEmpty()) {
                item { EmptyCard("Take the baseline vital signs first, then start a transfusion below.") }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(c.bg)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = TvsSpace.s12)
                .padding(top = TvsSpace.s8, bottom = TvsSpace.s8),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
        ) {
            UndoSlot()
            // Amber like the alarm itself; the patient's own action just below it is navy.
            if (ringing) Alert("Alarm ringing. Open it", R.drawable.ic_bell, c.alarmAction, c.alarmOnAction, null, onOpenAlarm)
            if (urgent != null) {
                if (urgent.roundOpen) {
                    PatientButton(urgent.bed, "Open round", onOpenAlarm, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
                } else {
                    PatientButton(urgent.bed, urgent.ackLabel, { onAcknowledge(urgent.ackIds) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
                }
            }
            if (anyTransfusion) ReactionBar(onClick = onReaction, minHeight = TvsSize.Touch, quiet = true, short = large)
            // Always here, whatever the font size: the same place for the thumb.
            StartActions(onStartTransfusion, onStartTurning, stacked = large, quiet = urgent != null)
        }
    }
}

private fun BedView.ownerKey() = if (type == BedType.TRANSFUSION) UndoAction.transfusion(id) else UndoAction.plan(id)

/**
 * Start is one action with two kinds, side by side so neither costs an extra screen. It steps back when
 * a bed needs something: then that bed's action is the primary one.
 */
@Composable
private fun StartActions(onStartTransfusion: () -> Unit, onStartTurning: () -> Unit, stacked: Boolean, quiet: Boolean) {
    val tagged = if (quiet) Modifier else Modifier.testTag(PRIMARY_TAG)
    if (stacked) {
        Column(Modifier.fillMaxWidth().then(tagged), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            StartTile("Start transfusion", R.drawable.ic_drop, onStartTransfusion, quiet, Modifier.fillMaxWidth())
            StartTile("Start turning", R.drawable.ic_turn, onStartTurning, quiet, Modifier.fillMaxWidth())
        }
    } else {
        Row(Modifier.fillMaxWidth().then(tagged), horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            StartTile("Start transfusion", R.drawable.ic_drop, onStartTransfusion, quiet, Modifier.weight(1f))
            StartTile("Start turning", R.drawable.ic_turn, onStartTurning, quiet, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StartTile(text: String, icon: Int, onClick: () -> Unit, quiet: Boolean, modifier: Modifier) {
    val c = Ward.c
    val ink = if (quiet) c.ink else c.onPrimary
    Column(
        modifier.heightIn(min = TvsSize.Touch)
            .background(if (quiet) c.card else c.primary, TvsShape.Card)
            .border(TvsSize.Edge, if (quiet) c.muted else c.primaryEdge, TvsShape.Card)
            .wardClick(onClick = onClick)
            .padding(horizontal = TvsSpace.s4, vertical = TvsSpace.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(TvsSize.IconSmall))
        Text(text, color = ink, style = WardType.Tag, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeBed(bed: BedView, undo: UndoAction?, onOpen: () -> Unit, onUndo: (Long) -> Unit) {
    BedCard(bed, onOpen) {
        if (undo != null) {
            Spacer(Modifier.height(TvsSpace.s6))
            SecondaryButton("Undo last", { onUndo(undo.id) }, Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch))
        }
    }
}

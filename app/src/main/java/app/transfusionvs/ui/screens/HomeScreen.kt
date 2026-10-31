package app.transfusionvs.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
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
import app.transfusionvs.ui.components.AckButton
import app.transfusionvs.ui.components.Alert
import app.transfusionvs.ui.components.BedCard
import app.transfusionvs.ui.components.EmptyCard
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.ReactionBar
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.UndoSlot
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.components.wardClick
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
 * urgent first, each with its next clock time. Quiet when all is well.
 * [anyTransfusion]: the reaction bar shows only when there is blood on the ward, running or finished
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

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = "${BedViews.running(beds)} · ${Labels.clock(now)}",
            title = BedViews.summary(beds),
            trailing = {
                Box(
                    Modifier.size(TvsSize.Touch).wardClick(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings", tint = c.onHeader, modifier = Modifier.size(TvsSize.IconMedium))
                }
            },
        )

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = TvsSpace.s12, vertical = TvsSpace.s4),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
        ) {
            if (ringing) {
                item { Alert("Alarm ringing — tap to open", c.dangerFill, c.onDangerFill, onOpenAlarm) }
            }
            if (setupProblems > 0) {
                item {
                    Alert(
                        "Setup check: $setupProblems item${if (setupProblems > 1) "s" else ""} to fix — alarms may not ring",
                        c.soon, c.onSoon, onSetup,
                    )
                }
            }
            items(beds, key = { "${it.type}${it.id}" }) { bed ->
                HomeBed(
                    bed = bed,
                    undo = live.firstOrNull { it.ownerKey == bed.ownerKey() },
                    onOpen = { if (bed.type == BedType.TRANSFUSION) onOpen(bed.id) else onOpenTurnPlan(bed.id) },
                    onOpenRound = onOpenAlarm,
                    onAcknowledge = onAcknowledge,
                    onUndo = onUndo,
                )
            }
            if (home != null && turns != null && beds.isEmpty()) {
                item { EmptyCard("No beds running", "Take the baseline vital signs first, then start a transfusion below.") }
            }
            if (large) {
                item { StartActions(onStartTransfusion, onStartTurning, stacked = true) }
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
            if (anyTransfusion) ReactionBar(onClick = onReaction, minHeight = TvsSize.Touch)
            if (!large) StartActions(onStartTransfusion, onStartTurning, stacked = false)
        }
    }
}

private fun BedView.ownerKey() = if (type == BedType.TRANSFUSION) UndoAction.transfusion(id) else UndoAction.plan(id)

/** Start is one action with two kinds, side by side so neither costs an extra screen. */
@Composable
private fun StartActions(onStartTransfusion: () -> Unit, onStartTurning: () -> Unit, stacked: Boolean) {
    if (stacked) {
        Column(Modifier.fillMaxWidth().testTag(PRIMARY_TAG), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            PrimaryButton("Start transfusion", onStartTransfusion, icon = R.drawable.ic_drop)
            PrimaryButton("Start turning", onStartTurning, icon = R.drawable.ic_turn)
        }
    } else {
        Row(Modifier.fillMaxWidth().testTag(PRIMARY_TAG), horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            PrimaryButton("Start transfusion", onStartTransfusion, Modifier.weight(1f), icon = R.drawable.ic_drop, minHeight = TvsSize.Touch, style = WardType.ButtonSmall)
            PrimaryButton("Start turning", onStartTurning, Modifier.weight(1f), icon = R.drawable.ic_turn, minHeight = TvsSize.Touch, style = WardType.ButtonSmall)
        }
    }
}

@Composable
private fun HomeBed(
    bed: BedView,
    undo: UndoAction?,
    onOpen: () -> Unit,
    onOpenRound: () -> Unit,
    onAcknowledge: (List<Long>) -> Unit,
    onUndo: (Long) -> Unit,
) {
    BedCard(bed, onOpen) {
        if (bed.ackIds.isNotEmpty() && !bed.stoppedForReaction) {
            Spacer(Modifier.height(TvsSpace.s6))
            AckButton("${bed.bed} · ${bed.ackLabel}", { onAcknowledge(bed.ackIds) })
        }
        if (bed.roundOpen) {
            Spacer(Modifier.height(TvsSpace.s6))
            AckButton("Open round", onOpenRound)
        }
        if (undo != null) {
            Spacer(Modifier.height(TvsSpace.s6))
            SecondaryButton("Undo last", { onUndo(undo.id) }, Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch))
        }
    }
}

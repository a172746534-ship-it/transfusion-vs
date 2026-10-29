package app.transfusionvs.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TimelineBuilder
import app.transfusionvs.schedule.Timeliness
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.schedule.TurnTimeline
import app.transfusionvs.setup.SetupChecks
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.components.ActionBlock
import app.transfusionvs.ui.components.Alert
import app.transfusionvs.ui.components.BedCard
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.EmptyCard
import app.transfusionvs.ui.components.NextLine
import app.transfusionvs.ui.components.PositionIcon
import app.transfusionvs.ui.components.ReactionBanner
import app.transfusionvs.ui.components.SectionLabel
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onStart: () -> Unit,
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
        ringing = ringing.isNotEmpty(),
        setupProblems = setupProblems,
        now = now,
        onStart = onStart,
        onOpen = onOpen,
        onOpenTurnPlan = onOpenTurnPlan,
        onReaction = onReaction,
        onSetup = onSetup,
        onSettings = onSettings,
        onOpenAlarm = openAlarm,
        onAcknowledge = vm::acknowledge,
    )
}

/** Stateless Home (rendered by the screenshot tests with fake data). */
@Composable
fun HomeContent(
    home: List<TransfusionWithCheckpoints>?,
    turns: List<TurnPlanWithSlots>?,
    ringing: Boolean,
    setupProblems: Int,
    now: Long,
    onStart: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenTurnPlan: (Long) -> Unit,
    onReaction: () -> Unit,
    onSetup: () -> Unit,
    onSettings: () -> Unit,
    onOpenAlarm: () -> Unit,
    onAcknowledge: (List<Long>) -> Unit,
) {
    val transfusions = home.orEmpty()
    val plans = turns.orEmpty()
    val transfusionRunning = transfusions.any { it.transfusion.status == TransfusionStatus.RUNNING }
    val turningRunning = plans.any { it.plan.status == TurnPlanStatus.RUNNING }

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        // Same header as always; it shows what is running (transfusion first).
        if (!transfusionRunning && turningRunning) {
            TopBar(caps = "Repositioning running", title = "Turns on the ward clock", icon = R.drawable.ic_turn)
        } else {
            TopBar(
                caps = if (transfusionRunning) "Blood transfusion running" else "Transfusion VS",
                title = "Vital signs → Cerner, on time",
                showDrop = true,
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(TvsSpace.s16),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s14),
        ) {
            if (ringing) {
                item { Alert("Alarm ringing — tap to open", Tvs.Red, onClick = onOpenAlarm) }
            }
            if (setupProblems > 0) {
                item {
                    Alert(
                        "Setup check: $setupProblems item${if (setupProblems > 1) "s" else ""} to fix — alarms may not ring",
                        Tvs.Amber, textColor = Tvs.Navy, onClick = onSetup,
                    )
                }
            }

            item { SectionLabel("Transfusions") }
            items(transfusions, key = { "t${it.transfusion.id}" }) { t ->
                TransfusionCard(t, now, onOpen = { onOpen(t.transfusion.id) }, onAcknowledge = onAcknowledge)
            }
            if (home != null && transfusions.isEmpty()) {
                item { EmptyCard("No transfusion running", "Take the baseline vital signs first, then tap Start.") }
            }

            item { SectionLabel("Repositioning") }
            items(plans, key = { "r${it.plan.id}" }) { p ->
                TurnCard(p, now, onOpen = { onOpenTurnPlan(p.plan.id) }, onOpenRound = onOpenAlarm)
            }
            if (turns != null && plans.isEmpty()) {
                item { EmptyCard("No repositioning plan", "Tap Start, then Repositioning, to turn beds on the ward clock.") }
            }

            item {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                    shape = TvsShape.Card,
                ) { Text("Settings", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold) }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Tvs.Panel)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = TvsSpace.s16, vertical = TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            ReactionBanner(onClick = onReaction)
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge),
                shape = TvsShape.Card,
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
            ) { Text("Start", fontSize = TvsFont.Heading, fontWeight = FontWeight.Black) }
        }
    }
}

/** One card per transfusion: bed + colour, mini timeline, next checkpoint with countdown, overdue in red. */
@Composable
private fun TransfusionCard(
    item: TransfusionWithCheckpoints,
    now: Long,
    onOpen: () -> Unit,
    onAcknowledge: (List<Long>) -> Unit,
) {
    val t = item.transfusion
    val rows = item.checkpoints
    val ringing = rows.filter { it.status == CheckpointStatus.RINGING }
    val missed = rows.filter { it.status == CheckpointStatus.MISSED && it.ackAt == null }
    val endPrompt = rows.filter { it.type == CheckpointType.END && it.status == CheckpointStatus.PENDING }
    val next = rows.filter { it.status == CheckpointStatus.PENDING && it.type.rings }.minByOrNull { it.scheduledAt }

    BedCard(
        colorIndex = t.colorIndex,
        title = t.bedLabel,
        subtitle = when (t.status) {
            TransfusionStatus.RUNNING -> "Started ${Labels.clock(t.startAt)} · planned ${Labels.duration(t.plannedDurationMin)}"
            TransfusionStatus.ENDED -> "Ended ${t.endAt?.let { Labels.clock(it) } ?: ""}"
            TransfusionStatus.STOPPED_REACTION -> "Stopped — reaction"
        },
        overdue = ringing.isNotEmpty() || missed.isNotEmpty(),
        onOpen = onOpen,
    ) {
        CardTimeline(TimelineBuilder.live(t, rows, now), compact = true)
        if (next != null && t.status == TransfusionStatus.RUNNING) {
            NextLine(
                "${Labels.title(next.type, next.offsetMin)} at ${Labels.clock(next.scheduledAt)}",
                Timeliness.countdown(next.scheduledAt - now),
            )
        }
        if (ringing.isNotEmpty()) {
            Text(
                "Ringing now: " + ringing.joinToString { Labels.title(it.type, it.offsetMin) },
                color = Tvs.Red, fontSize = TvsFont.Body, fontWeight = FontWeight.Black,
            )
        }
        if (missed.isNotEmpty()) {
            ActionBlock(
                text = "Overdue: " + missed.joinToString { "${Labels.title(it.type, it.offsetMin)} (${Labels.clock(it.scheduledAt)})" },
                background = Tvs.Red, textColor = Tvs.White,
                onAck = { onAcknowledge(missed.map { it.id }) },
            )
        }
        if (endPrompt.isNotEmpty()) {
            ActionBlock(
                text = "End VS — measure, then document in Cerner",
                background = Tvs.Amber, textColor = Tvs.Navy,
                onAck = { onAcknowledge(endPrompt.map { it.id }) },
            )
        }
    }
}

/**
 * One card per repositioning plan — also the handover summary: current position, last turn,
 * next slot with countdown, overdue in red (same tokens as transfusions).
 */
@Composable
private fun TurnCard(item: TurnPlanWithSlots, now: Long, onOpen: () -> Unit, onOpenRound: () -> Unit) {
    val p = item.plan
    val slots = item.slots
    val rotation = Position.parseList(p.rotation)
    val lastTurn = slots.filter { it.status == TurnSlotStatus.ACKED && it.ackAt != null }.maxByOrNull { it.ackAt!! }
    val open = slots.filter { it.status == TurnSlotStatus.RINGING }
    val next = slots.filter { it.status == TurnSlotStatus.PENDING && it.slotAt > now }.minByOrNull { it.slotAt }

    BedCard(
        colorIndex = p.colorIndex,
        title = p.bedLabel,
        subtitle = when (p.status) {
            TurnPlanStatus.RUNNING ->
                "Every ${Labels.duration(p.intervalMin)}" + (p.stopAt?.let { " · until ${Labels.clock(it)}" } ?: "")
            TurnPlanStatus.STOPPED -> "Stopped"
        },
        overdue = open.isNotEmpty(),
        onOpen = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PositionIcon(p.currentPosition, Tvs.Navy, TvsSize.IconMedium)
            Spacer(Modifier.width(TvsSpace.s10))
            Column {
                Text("Now: ${p.currentPosition.label}", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                Text(
                    lastTurn?.ackAt?.let { "Last turn ${Labels.clock(it)}" } ?: "No turn yet",
                    color = Tvs.Muted, fontSize = TvsFont.Caption,
                )
            }
        }
        CardTimeline(TurnTimeline.live(p, slots, now), compact = true)
        if (next != null && p.status == TurnPlanStatus.RUNNING && open.isEmpty()) {
            NextLine(
                "${TurnGrid.nextPosition(p.currentPosition, rotation).label} at ${Labels.clock(next.slotAt)}",
                Timeliness.countdown(next.slotAt - now),
            )
        }
        if (open.isNotEmpty()) {
            ActionBlock(
                text = "Overdue: " + open.joinToString { "${Labels.clock(it.slotAt)} turn" } + " — not done yet",
                background = Tvs.Red, textColor = Tvs.White,
                buttonText = "Open round",
                onAck = onOpenRound,
            )
        }
    }
}

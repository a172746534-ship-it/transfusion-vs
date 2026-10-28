package app.transfusionvs.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TimelineBuilder
import app.transfusionvs.schedule.Timeliness
import app.transfusionvs.setup.SetupChecks
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.ReactionBanner
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onStart: () -> Unit,
    onOpen: (Long) -> Unit,
    onReaction: () -> Unit,
    onSetup: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val home by vm.home.collectAsStateWithLifecycle()
    val ringing by vm.ringing.collectAsStateWithLifecycle()
    val now by rememberNow(15_000)
    var setupProblems by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        setupProblems = SetupChecks.evaluate(context).count { !it.ok }
        onPauseOrDispose { }
    }
    HomeContent(
        home = home,
        ringing = ringing.isNotEmpty(),
        setupProblems = setupProblems,
        now = now,
        onStart = onStart,
        onOpen = onOpen,
        onReaction = onReaction,
        onSetup = onSetup,
        onSettings = onSettings,
        onOpenAlarm = {
            context.startActivity(Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        onAcknowledge = vm::acknowledge,
    )
}

/** Stateless Home (rendered by the screenshot tests with fake data). */
@Composable
fun HomeContent(
    home: List<TransfusionWithCheckpoints>?,
    ringing: Boolean,
    setupProblems: Int,
    now: Long,
    onStart: () -> Unit,
    onOpen: (Long) -> Unit,
    onReaction: () -> Unit,
    onSetup: () -> Unit,
    onSettings: () -> Unit,
    onOpenAlarm: () -> Unit,
    onAcknowledge: (List<Long>) -> Unit,
) {
    val list = home.orEmpty()
    val anyRunning = list.any { it.transfusion.status == TransfusionStatus.RUNNING }

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(
            caps = if (anyRunning) "Blood transfusion running" else "Transfusion VS",
            title = "Vital signs → Cerner, on time",
            showDrop = true,
        )

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(TvsSpace.s16),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s14),
        ) {
            if (ringing) {
                item {
                    Alert("Alarm ringing — tap to open", Tvs.Red, onClick = onOpenAlarm)
                }
            }
            if (setupProblems > 0) {
                item {
                    Alert(
                        "Setup check: $setupProblems item${if (setupProblems > 1) "s" else ""} to fix — alarms may not ring",
                        Tvs.Amber, textColor = Tvs.Navy, onClick = onSetup,
                    )
                }
            }
            items(list, key = { it.transfusion.id }) { t ->
                TransfusionCard(t, now, onOpen = { onOpen(t.transfusion.id) }, onAcknowledge = onAcknowledge)
            }
            if (home != null && list.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(Tvs.White, TvsShape.Card).padding(TvsSpace.s20),
                        verticalArrangement = Arrangement.spacedBy(TvsSpace.s6),
                    ) {
                        Text("No transfusion running", color = Tvs.Navy, fontSize = TvsFont.Heading, fontWeight = FontWeight.Black)
                        Text(
                            "Take the baseline vital signs first, then tap Start transfusion.",
                            color = Tvs.Muted, fontSize = TvsFont.BodySmall,
                        )
                    }
                }
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
            ) { Text("Start transfusion", fontSize = TvsFont.Heading, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun Alert(text: String, color: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color = Tvs.White, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch).background(color, TvsShape.Card)
            .clickable(onClick = onClick).padding(TvsSpace.s16),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = textColor, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Black) }
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
    val overdue = ringing.isNotEmpty() || missed.isNotEmpty()
    val shape = TvsShape.Card

    Column(
        Modifier.fillMaxWidth()
            .background(Tvs.White, shape)
            .border(if (overdue) TvsSpace.s3 else 0.dp, if (overdue) Tvs.Red else Tvs.White, shape)
            .clickable(onClick = onOpen)
            .padding(TvsSpace.s16),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(TvsSize.DotLarge).background(Tvs.bedColor(t.colorIndex), CircleShape))
            Spacer(Modifier.width(TvsSpace.s10))
            Column(Modifier.weight(1f)) {
                Text(t.bedLabel, color = Tvs.Navy, fontSize = TvsFont.Title, fontWeight = FontWeight.Black)
                Text(
                    when (t.status) {
                        TransfusionStatus.RUNNING ->
                            "Started ${Labels.clock(t.startAt)} · planned ${Labels.duration(t.plannedDurationMin)}"
                        TransfusionStatus.ENDED -> "Ended ${t.endAt?.let { Labels.clock(it) } ?: ""}"
                        TransfusionStatus.STOPPED_REACTION -> "Stopped — reaction"
                    },
                    color = Tvs.Muted, fontSize = TvsFont.Caption,
                )
            }
            Text("›", color = Tvs.Muted, fontSize = TvsFont.Glyph)
        }

        CardTimeline(TimelineBuilder.live(t, rows, now), compact = true)

        if (next != null && t.status == TransfusionStatus.RUNNING) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next: ", color = Tvs.Muted, fontSize = TvsFont.Body)
                Text(
                    "${Labels.title(next.type, next.offsetMin)} at ${Labels.clock(next.scheduledAt)}",
                    color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold, style = TimeFigures,
                )
                Spacer(Modifier.width(TvsSpace.s8))
                Text(
                    Timeliness.countdown(next.scheduledAt - now),
                    color = Tvs.Blue, fontSize = TvsFont.Body, fontWeight = FontWeight.Black, style = TimeFigures,
                )
            }
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

@Composable
private fun ActionBlock(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onAck: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(background, TvsShape.Control).padding(TvsSpace.s12),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
    ) {
        Text(text, color = textColor, fontSize = TvsFont.Body, fontWeight = FontWeight.Black)
        Button(
            onClick = onAck,
            modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
            shape = TvsShape.Control,
            colors = ButtonDefaults.buttonColors(containerColor = Tvs.White, contentColor = Tvs.Navy),
        ) { Text("Measured & documented ✓", fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Black) }
    }
}

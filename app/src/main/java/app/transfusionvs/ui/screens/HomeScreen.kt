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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        Modifier.fillMaxWidth().background(Tvs.White, RoundedCornerShape(16.dp)).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("No transfusion running", color = Tvs.Navy, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Take the baseline vital signs first, then tap Start transfusion.",
                            color = Tvs.Muted, fontSize = 15.sp,
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Settings", color = Tvs.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Tvs.Panel)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReactionBanner(onClick = onReaction)
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
            ) { Text("Start transfusion", fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun Alert(text: String, color: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color = Tvs.White, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).background(color, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Black) }
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
    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier.fillMaxWidth()
            .background(Tvs.White, shape)
            .border(if (overdue) 3.dp else 0.dp, if (overdue) Tvs.Red else Tvs.White, shape)
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).background(Tvs.bedColor(t.colorIndex), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.bedLabel, color = Tvs.Navy, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    when (t.status) {
                        TransfusionStatus.RUNNING ->
                            "Started ${Labels.clock(t.startAt)} · planned ${Labels.duration(t.plannedDurationMin)}"
                        TransfusionStatus.ENDED -> "Ended ${t.endAt?.let { Labels.clock(it) } ?: ""}"
                        TransfusionStatus.STOPPED_REACTION -> "Stopped — reaction"
                    },
                    color = Tvs.Muted, fontSize = 14.sp,
                )
            }
            Text("›", color = Tvs.Muted, fontSize = 28.sp)
        }

        CardTimeline(TimelineBuilder.live(t, rows, now), compact = true)

        if (next != null && t.status == TransfusionStatus.RUNNING) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next: ", color = Tvs.Muted, fontSize = 16.sp)
                Text(
                    "${Labels.title(next.type, next.offsetMin)} at ${Labels.clock(next.scheduledAt)}",
                    color = Tvs.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = TimeFigures,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Timeliness.countdown(next.scheduledAt - now),
                    color = Tvs.Blue, fontSize = 16.sp, fontWeight = FontWeight.Black, style = TimeFigures,
                )
            }
        }

        if (ringing.isNotEmpty()) {
            Text(
                "Ringing now: " + ringing.joinToString { Labels.title(it.type, it.offsetMin) },
                color = Tvs.Red, fontSize = 16.sp, fontWeight = FontWeight.Black,
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
        Modifier.fillMaxWidth().background(background, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Button(
            onClick = onAck,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Tvs.White, contentColor = Tvs.Navy),
        ) { Text("Measured & documented ✓", fontSize = 17.sp, fontWeight = FontWeight.Black) }
    }
}

package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.StatusKind
import app.transfusionvs.schedule.TimelineBuilder
import app.transfusionvs.schedule.Timeliness
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.HourlyPill
import app.transfusionvs.ui.components.MeasureDocumentBlocks
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.StatusPill
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs

/** Full timeline in the card layout, every checkpoint with its status, and End / Edit / Stopped. */
@Composable
fun DetailScreen(
    vm: MainViewModel,
    id: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReaction: () -> Unit,
) {
    val flow = remember(id) { vm.transfusion(id) }
    val item by flow.collectAsStateWithLifecycle(initialValue = null)
    val now by rememberNow(15_000)
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    var laterEndVs by rememberSaveable { mutableStateOf(false) }

    val t = item?.transfusion
    val rows = item?.checkpoints.orEmpty()

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(
            caps = when (t?.status) {
                TransfusionStatus.RUNNING -> "Blood transfusion running"
                TransfusionStatus.ENDED -> "Ended"
                TransfusionStatus.STOPPED_REACTION -> "Stopped — reaction"
                null -> ""
            },
            title = t?.bedLabel ?: "",
            subtitle = t?.let { subtitle(it) },
            onBack = onBack,
            showDrop = t?.status == TransfusionStatus.RUNNING,
        )
        if (t == null) return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Panel(background = Tvs.White) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BedDot(t.colorIndex)
                    Spacer(Modifier.width(8.dp))
                    CapsLabel("Time from start", Tvs.Navy)
                    Text(" · baseline first", color = Tvs.Muted, fontSize = 13.sp)
                }
                HourlyPill()
                CardTimeline(TimelineBuilder.live(t, rows, now))
            }

            Panel {
                CapsLabel("Checkpoints", Tvs.Navy)
                visibleRows(t, rows, now).forEach { c ->
                    CheckpointRow(c, now, vm.toleranceMin, onAck = { vm.acknowledge(listOf(c.id)) })
                }
            }

            if (t.status == TransfusionStatus.RUNNING) {
                Button(
                    onClick = { confirmEnd = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
                ) { Text("End transfusion", fontSize = 19.sp, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Edit start time or duration", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Tvs.Navy) }
                Button(
                    onClick = onReaction,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tvs.Red, contentColor = Tvs.White),
                ) { Text("⚠ Transfusion stopped (reaction)", fontSize = 17.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    if (confirmEnd && t != null) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End ${t.bedLabel} now?") },
            text = { Text("End time ${Labels.clock(System.currentTimeMillis())} is logged and the remaining alarms are cancelled.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.end(t.id)
                    confirmEnd = false
                    laterEndVs = false
                }) { Text("End transfusion", fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancel") } },
        )
    }

    // The End VS prompt, shown right after End until documented (or "Later": it stays on Home).
    val endVs = rows.firstOrNull { it.type == CheckpointType.END && it.status == CheckpointStatus.PENDING }
    if (endVs != null && !laterEndVs && t != null) {
        AlertDialog(
            onDismissRequest = { laterEndVs = true },
            title = { Text("End VS — ${t.bedLabel}", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Transfusion ended at ${Labels.clock(endVs.scheduledAt)}.")
                    MeasureDocumentBlocks()
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.acknowledge(listOf(endVs.id)) },
                    modifier = Modifier.heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tvs.Amber, contentColor = Tvs.Navy),
                ) { Text("Measured & documented ✓", fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { laterEndVs = true }) { Text("Later") } },
        )
    }
}

private fun subtitle(t: Transfusion): String = when (t.status) {
    TransfusionStatus.RUNNING ->
        "Started ${Labels.clock(t.startAt)} · planned ${Labels.duration(t.plannedDurationMin)} " +
            "(ends ${Labels.clock(t.startAt + t.plannedDurationMin * MINUTE_MS)})"
    TransfusionStatus.ENDED -> "Started ${Labels.clock(t.startAt)} · ended ${t.endAt?.let { Labels.clock(it) } ?: ""}"
    TransfusionStatus.STOPPED_REACTION -> "Started ${Labels.clock(t.startAt)} · stopped ${t.endAt?.let { Labels.clock(it) } ?: ""}"
}

/** Hide cancelled rows and far-future hourly checks past the planned end (they appear as they come up). */
private fun visibleRows(t: Transfusion, rows: List<Checkpoint>, now: Long): List<Checkpoint> =
    rows.filter { it.status != CheckpointStatus.CANCELLED }
        .filter { it.offsetMin <= t.plannedDurationMin || it.status != CheckpointStatus.PENDING || it.scheduledAt <= now + 60 * MINUTE_MS || it.type == CheckpointType.LIMIT_WARNING }
        .sortedBy { it.scheduledAt }

@Composable
private fun CheckpointRow(c: Checkpoint, now: Long, toleranceMin: Int, onAck: () -> Unit) {
    val status = Timeliness.status(c, now, toleranceMin)
    val needsAck = c.status == CheckpointStatus.RINGING ||
        (c.status == CheckpointStatus.MISSED && c.ackAt == null) ||
        (c.type == CheckpointType.END && c.status == CheckpointStatus.PENDING)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Labels.clock(c.scheduledAt),
                style = TimeFigures, color = Tvs.Navy, fontSize = 18.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.width(64.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(Labels.title(c.type, c.offsetMin), color = Tvs.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (c.type == CheckpointType.PLANNED_END || c.type == CheckpointType.LIMIT_WARNING) {
                    Text(Labels.subtitle(c.type), color = Tvs.Muted, fontSize = 13.sp)
                }
                if (c.ackAt != null && c.type != CheckpointType.START) {
                    Text("Documented ${Labels.clock(c.ackAt)}", color = Tvs.Muted, fontSize = 13.sp)
                }
            }
            if (c.type != CheckpointType.START) StatusPill(status) else StatusPill("logged", Tvs.Navy, Tvs.White)
        }
        if (needsAck) {
            Button(
                onClick = onAck,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Amber, contentColor = Tvs.Navy),
            ) { Text("Measured & documented ✓", fontSize = 16.sp, fontWeight = FontWeight.Black) }
        }
    }
}

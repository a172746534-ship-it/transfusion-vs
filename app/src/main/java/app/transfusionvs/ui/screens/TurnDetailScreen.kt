package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.transfusionvs.R
import app.transfusionvs.data.Position
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.schedule.TurnStatus
import app.transfusionvs.schedule.TurnTimeline
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.HourlyPill
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PositionChange
import app.transfusionvs.ui.components.PositionIcon
import app.transfusionvs.ui.components.StatusPill
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace

/** Mirrors the transfusion Detail: timeline, the turns with their status, and Stop / Edit / Restart. */
@Composable
fun TurnDetailContent(
    item: TurnPlanWithSlots?,
    now: Long,
    windowMin: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStop: (Long) -> Unit,
    onRestart: (Long) -> Unit,
    onOpenRound: () -> Unit,
) {
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    val p = item?.plan
    val slots = item?.slots.orEmpty()
    val rotation = p?.let { Position.parseList(it.rotation) }.orEmpty()

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(
            caps = when (p?.status) {
                TurnPlanStatus.RUNNING -> "Repositioning running"
                TurnPlanStatus.STOPPED -> "Stopped"
                null -> ""
            },
            title = p?.bedLabel ?: "",
            subtitle = p?.let {
                "Every ${Labels.duration(it.intervalMin)} · first turn ${Labels.clock(it.firstSlotAt)}" +
                    (it.stopAt?.let { s -> " · until ${Labels.clock(s)}" } ?: "")
            },
            onBack = onBack,
            icon = if (p?.status == TurnPlanStatus.RUNNING) R.drawable.ic_turn else null,
        )
        if (p == null) return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(TvsSpace.s16),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            Panel(background = Tvs.White) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BedDot(p.colorIndex)
                    Spacer(Modifier.width(TvsSpace.s8))
                    CapsLabel("Ward clock", Tvs.Navy)
                    Text(" · late turns don't move it", color = Tvs.Muted, fontSize = TvsFont.Label)
                }
                HourlyPill("Every ${Labels.duration(p.intervalMin)} on the ward clock")
                CardTimeline(TurnTimeline.live(p, slots, now))
            }

            // Handover summary: position now, next turn, and the longest gap without a turn.
            Panel {
                CapsLabel("Now", Tvs.Navy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PositionIcon(p.currentPosition, Tvs.Navy, TvsSize.IconXL)
                    Spacer(Modifier.width(TvsSpace.s12))
                    Column(Modifier.weight(1f)) {
                        Text("Lying: ${p.currentPosition.label}", color = Tvs.Navy, fontSize = TvsFont.Lead, fontWeight = FontWeight.Black)
                        val next = slots.filter { it.status == TurnSlotStatus.PENDING && it.slotAt > now }.minByOrNull { it.slotAt }
                        if (next != null && p.status == TurnPlanStatus.RUNNING) {
                            Text(
                                "Next: ${TurnGrid.nextPosition(p.currentPosition, rotation).label} at ${Labels.clock(next.slotAt)}",
                                color = Tvs.Muted, fontSize = TvsFont.Caption,
                            )
                        }
                    }
                }
                val turnTimes = slots.filter { it.status == TurnSlotStatus.ACKED }.mapNotNull { it.ackAt }
                val since = maxOf(p.startAt, now - 24 * 60 * MINUTE_MS)
                val gap = TurnGrid.longestGapMs(turnTimes, since, now)
                val tooLong = gap > (p.intervalMin + windowMin) * MINUTE_MS
                Row(
                    Modifier.fillMaxWidth().background(Tvs.Panel, TvsShape.Control).padding(TvsSpace.s12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Longest gap without a turn (24 h)",
                        color = Tvs.Muted, fontSize = TvsFont.Caption, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    )
                    Text(
                        TurnStatus.gap(gap),
                        style = TimeFigures, color = if (tooLong) Tvs.Red else Tvs.Navy, fontSize = TvsFont.Number, fontWeight = FontWeight.Black,
                    )
                }
            }

            Panel {
                CapsLabel("Turns", Tvs.Navy)
                slots.filter { it.status != TurnSlotStatus.CANCELLED && (it.status != TurnSlotStatus.PENDING || it.slotAt <= now + 4 * 60 * MINUTE_MS) }
                    .sortedBy { it.slotAt }
                    .forEach { s -> TurnRow(s, now, windowMin, onOpenRound) }
            }

            if (p.status == TurnPlanStatus.RUNNING) {
                Button(
                    onClick = { confirmStop = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge),
                    shape = TvsShape.Card,
                    colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
                ) { Text("Stop plan", fontSize = TvsFont.Button, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                    shape = TvsShape.Card,
                ) { Text("Edit positions, interval or stop time", fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Bold, color = Tvs.Navy) }
            } else {
                Button(
                    onClick = { onRestart(p.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge),
                    shape = TvsShape.Card,
                    colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
                ) { Text("Restart plan", fontSize = TvsFont.Button, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    if (confirmStop && p != null) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop turning ${p.bedLabel}?") },
            text = { Text("No more turn alarms for this bed. You can restart the plan any time.") },
            confirmButton = {
                TextButton(onClick = {
                    onStop(p.id)
                    confirmStop = false
                }) { Text("Stop plan", fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TurnRow(s: TurnSlot, now: Long, windowMin: Int, onOpenRound: () -> Unit) {
    val status = TurnStatus.of(s, now, windowMin)
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Labels.clock(s.slotAt),
                style = TimeFigures, color = Tvs.Navy, fontSize = TvsFont.Lead, fontWeight = FontWeight.Black,
                modifier = Modifier.width(TvsSize.TimeColumn),
            )
            Column(Modifier.weight(1f)) {
                val to = s.positionTo
                if (s.status == TurnSlotStatus.ACKED && to != null) {
                    PositionChange(s.positionFrom, to, Tvs.Navy, TvsSize.IconSmall, TvsFont.Caption)
                    s.ackAt?.let { Text("Turned ${Labels.clock(it)}", color = Tvs.Muted, fontSize = TvsFont.Label) }
                } else {
                    Text("Turn", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                }
            }
            StatusPill(status)
        }
        if (s.status == TurnSlotStatus.RINGING) {
            Button(
                onClick = onOpenRound,
                modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                shape = TvsShape.Control,
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Amber, contentColor = Tvs.Navy),
            ) { Text("Open round", fontSize = TvsFont.Body, fontWeight = FontWeight.Black) }
        }
    }
}

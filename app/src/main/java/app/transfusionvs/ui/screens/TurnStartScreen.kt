package app.transfusionvs.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.transfusionvs.data.Position
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.schedule.TurnTimeline
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.HourlyPill
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PositionIcon
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.Tvs
import app.transfusionvs.ui.theme.TvsFont
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import java.time.Instant
import java.time.ZoneId

private const val MAX_BED_LABEL = 16

/** What the Start screen hands over to the engine. */
data class TurnPlanDraft(
    val bedLabel: String,
    val intervalMin: Int,
    val rotation: List<Position>,
    val current: Position,
    val firstSlotAt: Long,
    val stopAt: Long?,
)

/**
 * Start repositioning, or Edit when [editing] is given. Mirrors the transfusion Start screen:
 * the same panels, chips and timeline preview, with ward-clock slots instead of minutes from start.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TurnStartContent(
    editing: TurnPlan?,
    defaultIntervalMin: Int,
    openedAt: Long,
    onSubmit: (TurnPlanDraft) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()

    var bed by rememberSaveable { mutableStateOf(editing?.bedLabel ?: "") }
    var interval by rememberSaveable { mutableIntStateOf(editing?.intervalMin ?: defaultIntervalMin) }
    // Positions in rotation order; [included] marks the allowed ones (excluded = never suggested).
    var order by rememberSaveable {
        mutableStateOf(
            Position.toCsv(
                editing?.let { Position.parseList(it.rotation) + Position.entries.filter { p -> p !in Position.parseList(it.rotation) } }
                    ?: (Position.DEFAULT_ROTATION + listOf(Position.PRONE, Position.HOB_UP)),
            ),
        )
    }
    var included by rememberSaveable {
        mutableStateOf(editing?.rotation ?: Position.toCsv(Position.DEFAULT_ROTATION))
    }
    val orderList = Position.parseList(order)
    val rotation = orderList.filter { it in Position.parseList(included) }
    var current by rememberSaveable { mutableStateOf(editing?.currentPosition ?: Position.BACK) }
    val currentOk = if (current in rotation) current else rotation.firstOrNull() ?: Position.BACK

    val choices = TurnGrid.firstSlotChoices(openedAt, interval, zone)
    val defaultFirst = TurnGrid.firstSlot(openedAt, interval, zone)
    var firstSlotPick by rememberSaveable { mutableStateOf<Long?>(editing?.firstSlotAt) }
    val firstSlot = firstSlotPick?.takeIf { it in choices } ?: defaultFirst
    val shiftEnd = TurnGrid.endOfShift(openedAt, zone)
    var stopChoice by rememberSaveable { mutableIntStateOf(if (editing?.stopAt != null) 2 else 0) } // 0 none, 1 shift, 2 picked
    var pickedStop by rememberSaveable { mutableStateOf(editing?.stopAt) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val stopAt = when (stopChoice) {
        1 -> shiftEnd
        2 -> pickedStop
        else -> null
    }

    val canSubmit = bed.isNotBlank() && rotation.isNotEmpty() && (stopAt == null || stopAt > firstSlot)

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(
            caps = if (editing == null) "New repositioning plan" else "Edit",
            title = if (editing == null) "Start repositioning" else "Edit ${editing.bedLabel}",
            onBack = onBack,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(TvsSpace.s16),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            // Bed — no patient identifiers
            Panel {
                CapsLabel("Bed", Tvs.Navy)
                OutlinedTextField(
                    value = bed,
                    onValueChange = { bed = it.take(MAX_BED_LABEL) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = TvsFont.Number, fontWeight = FontWeight.Bold, color = Tvs.Navy),
                    placeholder = { Text("e.g. Bed 12", fontSize = TvsFont.Number) },
                    supportingText = { Text("Bed only — no names or MRN", color = Tvs.Red, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            // Interval on the ward clock
            Panel {
                CapsLabel("Turn every", Tvs.Navy)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    TurnGrid.INTERVALS.forEach { m ->
                        ChoiceChip(Labels.duration(m), interval == m, { interval = m; firstSlotPick = null }, Modifier.weight(1f))
                    }
                }
                Text("Slots stay on the ward clock (08:00 and 20:00 are always slots).", color = Tvs.Muted, fontSize = TvsFont.Caption)
            }

            // Positions: include / exclude and the rotation order
            Panel {
                CapsLabel("Positions", Tvs.Navy)
                Text("Untick a position this bed must not use. Arrows change the rotation order.", color = Tvs.Muted, fontSize = TvsFont.Caption)
                orderList.forEachIndexed { i, p ->
                    val on = p in rotation
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = on,
                            onCheckedChange = { checked ->
                                val now = Position.parseList(included)
                                included = Position.toCsv(if (checked) now + p else now - p)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Tvs.Navy),
                        )
                        PositionIcon(p, if (on) Tvs.Navy else Tvs.Outline, TvsSize.IconMedium)
                        Spacer(Modifier.width(TvsSpace.s10))
                        Column(Modifier.weight(1f)) {
                            Text(p.label, color = if (on) Tvs.Navy else Tvs.Muted, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                            if (!on) Text("not for this bed", color = Tvs.Muted, fontSize = TvsFont.Label)
                        }
                        OrderButton("↑", enabled = i > 0) { order = Position.toCsv(orderList.swap(i, i - 1)) }
                        Spacer(Modifier.width(TvsSpace.s6))
                        OrderButton("↓", enabled = i < orderList.lastIndex) { order = Position.toCsv(orderList.swap(i, i + 1)) }
                    }
                }
                Text(
                    "Rotation: " + (rotation.joinToString(" → ") { it.label }.ifEmpty { "choose at least one" }) + if (rotation.size > 1) " → …" else "",
                    color = Tvs.Navy, fontSize = TvsFont.Caption, fontWeight = FontWeight.Bold,
                )
            }

            // Where the patient lies now
            Panel {
                CapsLabel("Lying now", Tvs.Navy)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    rotation.forEach { p -> ChoiceChip(p.label, currentOk == p, { current = p }) }
                }
            }

            // First turn: the next slot, or the one after if it is under 30 min away
            Panel {
                CapsLabel("First turn", Tvs.Navy)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    choices.forEach { slot ->
                        ChoiceChip(Labels.clock(slot), firstSlot == slot, { firstSlotPick = slot }, Modifier.weight(1f))
                    }
                }
                if (defaultFirst != choices.first()) {
                    Text(
                        "${Labels.clock(choices.first())} is less than ${TurnGrid.MIN_LEAD_MIN} min away, so the first turn is ${Labels.clock(defaultFirst)}.",
                        color = Tvs.Muted, fontSize = TvsFont.Caption,
                    )
                }
            }

            // Optional stop time
            Panel {
                CapsLabel("Stop", Tvs.Navy)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    ChoiceChip("No stop time", stopChoice == 0, { stopChoice = 0 })
                    ChoiceChip("End of my shift (${Labels.clock(shiftEnd)})", stopChoice == 1, { stopChoice = 1 })
                    ChoiceChip(
                        if (stopChoice == 2 && pickedStop != null) "At ${Labels.clock(pickedStop!!)}" else "Pick time…",
                        stopChoice == 2,
                        { showPicker = true },
                    )
                }
                Text("No alarms at or after the stop time. You can restart the plan any time.", color = Tvs.Muted, fontSize = TvsFont.Caption)
            }

            // Preview with real clock times
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CapsLabel("Ward clock", Tvs.Navy)
                    Text(" · late turns don't move it", color = Tvs.Muted, fontSize = TvsFont.Label)
                }
                HourlyPill("Every ${Labels.duration(interval)} on the ward clock")
                CardTimeline(TurnTimeline.preview(openedAt, currentOk, rotation, firstSlot, interval, stopAt, zone))
            }
        }

        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(TvsSpace.s16)) {
            Button(
                onClick = {
                    onSubmit(TurnPlanDraft(bed, interval, rotation, currentOk, firstSlot, stopAt))
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchLarge),
                shape = TvsShape.Card,
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
            ) {
                Text(if (editing == null) "Start repositioning" else "Save changes", fontSize = TvsFont.Button, fontWeight = FontWeight.Black)
            }
        }
    }

    if (showPicker) {
        val initial = Instant.ofEpochMilli(pickedStop ?: shiftEnd).atZone(zone)
        val state = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Stop turning at") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    pickedStop = Labels.nextOccurrence(state.hour, state.minute, System.currentTimeMillis(), zone)
                    stopChoice = 2
                    showPicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OrderButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(TvsSize.Touch),
        shape = TvsShape.Control,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
    ) { Text(label, fontSize = TvsFont.Lead, fontWeight = FontWeight.Black, color = if (enabled) Tvs.Navy else Tvs.Outline) }
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> = toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

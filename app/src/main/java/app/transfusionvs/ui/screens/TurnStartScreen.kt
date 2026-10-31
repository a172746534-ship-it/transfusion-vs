package app.transfusionvs.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.data.Position
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.schedule.TurnTimeline
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.BedGrid
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.ChangeRow
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.ChoiceRow
import app.transfusionvs.ui.components.OTHER_BED
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PositionIcon
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.wardClick
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import androidx.compose.foundation.border
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

/** What the Start screen hands over to the engine. */
data class TurnPlanDraft(
    val bedLabel: String,
    val intervalMin: Int,
    val rotation: List<Position>,
    val current: Position,
    val firstSlotAt: Long,
    val stopAt: Long?,
)

@Composable
fun TurnStartScreen(vm: MainViewModel, editing: TurnPlan?, onSubmit: (TurnPlanDraft) -> Unit, onBack: () -> Unit) {
    val openedAt = rememberSaveable { System.currentTimeMillis() }
    val beds by vm.wardBeds.collectAsStateWithLifecycle()
    val busy by vm.busyBeds.collectAsStateWithLifecycle()
    TurnStartContent(
        editing = editing, beds = beds, busy = busy,
        defaultIntervalMin = editing?.intervalMin ?: vm.turnIntervalMin, openedAt = openedAt,
        onSubmit = { draft ->
            if (draft.bedLabel !in beds) vm.addWardBeds(listOf(draft.bedLabel))
            onSubmit(draft)
        },
        onBack = onBack,
    )
}

/**
 * Start repositioning, or Edit when [editing] is given. Pick a bed, then tap the position the patient
 * lies in now: that tap IS what starts the plan. Everything else has a default and sits behind
 * "More options", so changing it is possible and never required.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TurnStartContent(
    editing: TurnPlan?,
    beds: List<String>,
    busy: Map<String, Int>,
    defaultIntervalMin: Int,
    openedAt: Long,
    onSubmit: (TurnPlanDraft) -> Unit,
    onBack: () -> Unit,
    /** For screenshots: options open from the start. */
    initiallyExpanded: Boolean = false,
    /** For screenshots: a bed already picked. */
    initialBed: String = "",
) {
    val context = LocalContext.current
    val c = Ward.c
    val zone = ZoneId.systemDefault()

    var picked by rememberSaveable { mutableStateOf(editing?.bedLabel ?: initialBed) }
    var otherText by rememberSaveable { mutableStateOf("") }
    val bed = if (picked == OTHER_BED) otherText.trim() else picked

    var options by rememberSaveable { mutableStateOf(initiallyExpanded || editing != null) }
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
    var included by rememberSaveable { mutableStateOf(editing?.rotation ?: Position.toCsv(Position.DEFAULT_ROTATION)) }
    val orderList = Position.parseList(order)
    val rotation = orderList.filter { it in Position.parseList(included) }
    var current by rememberSaveable { mutableStateOf(editing?.currentPosition ?: Position.BACK) }
    val currentOk = if (current in rotation) current else rotation.firstOrNull() ?: Position.BACK

    val choices = TurnGrid.firstSlotChoices(openedAt, interval, zone)
    val defaultFirst = TurnGrid.firstSlot(openedAt, interval, zone)
    var firstSlotPick by rememberSaveable { mutableStateOf(editing?.firstSlotAt) }
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
    val validPlan = bed.isNotBlank() && rotation.isNotEmpty() && (stopAt == null || stopAt > firstSlot)

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = if (editing == null) "New turning plan" else "Edit",
            title = if (editing == null) "Start turning" else "Edit ${editing.bedLabel}",
            onBack = onBack,
            icon = R.drawable.ic_turn,
        )
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        Column(
            Modifier.weight(1f).verticalScroll(scroll).imePadding().padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            Panel {
                CapsLabel("Which bed?")
                BedGrid(
                    beds = (beds + listOfNotNull(editing?.bedLabel)).distinct(), busy = busy, selected = picked, otherText = otherText,
                    onPick = { picked = it }, onOtherText = { otherText = it },
                )
            }

            Panel {
                if (defaultFirst != choices.first() && firstSlot == defaultFirst) {
                    Text(
                        "${Labels.clock(choices.first())} is less than ${TurnGrid.MIN_LEAD_MIN} min away, so the first turn is ${Labels.clock(defaultFirst)}.",
                        color = c.muted, style = WardType.Body,
                    )
                }
                if (editing == null) {
                    SecondaryButton(if (options) "Fewer options" else "More options", { options = !options })
                }
            }

            if (options) {
                // Interval on the ward clock
                Panel {
                    CapsLabel("Turn every")
                    ChoiceRow(
                        TurnGrid.INTERVALS.map { Labels.duration(it) },
                        TurnGrid.INTERVALS.indexOf(interval),
                        {
                            interval = TurnGrid.INTERVALS[it]
                            firstSlotPick = null
                        },
                    )
                    Text("08:00 and 20:00 are always turn times.", color = c.muted, style = WardType.Body)
                }

                // Positions: include / exclude and the rotation order
                Panel {
                    CapsLabel("Positions")
                    Text("Untick a position this bed must not use. Arrows change the rotation order.", color = c.muted, style = WardType.Body)
                    orderList.forEachIndexed { i, p ->
                        val on = p in rotation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = on,
                                onCheckedChange = { checked ->
                                    val now = Position.parseList(included)
                                    included = Position.toCsv(if (checked) now + p else now - p)
                                },
                                modifier = Modifier.size(TvsSize.Touch).semantics { contentDescription = p.label },
                                colors = CheckboxDefaults.colors(checkedColor = c.primary, checkmarkColor = c.onPrimary, uncheckedColor = c.muted),
                            )
                            PositionIcon(p, if (on) c.ink else c.muted, TvsSize.IconMedium)
                            Spacer(Modifier.width(TvsSpace.s6))
                            Column(Modifier.weight(1f)) {
                                Text(p.label, color = if (on) c.ink else c.muted, style = WardType.BodyBold)
                                if (!on) Text("not for this bed", color = c.muted, style = WardType.Body)
                            }
                            OrderButton("↑", enabled = i > 0, description = "Move ${p.label} up") { order = Position.toCsv(orderList.swap(i, i - 1)) }
                            Spacer(Modifier.width(TvsSpace.s8))
                            OrderButton("↓", enabled = i < orderList.lastIndex, description = "Move ${p.label} down") { order = Position.toCsv(orderList.swap(i, i + 1)) }
                        }
                    }
                    Text(
                        "Rotation: " + (rotation.joinToString(" → ") { it.label }.ifEmpty { "choose at least one" }) + if (rotation.size > 1) " → …" else "",
                        color = c.ink, style = WardType.BodyBold,
                    )
                }

                // First turn: the next slot, or the one after if it is under 30 min away
                Panel {
                    CapsLabel("First turn")
                    ChoiceRow(choices.map { Labels.clock(it) }, choices.indexOf(firstSlot), { firstSlotPick = choices[it] })
                }

                // Optional stop time
                Panel {
                    CapsLabel("Stop")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                        ChoiceChip("No stop time", stopChoice == 0, { stopChoice = 0 })
                        ChoiceChip("End of my shift (${Labels.clock(shiftEnd)})", stopChoice == 1, { stopChoice = 1 })
                        ChoiceChip(
                            if (stopChoice == 2 && pickedStop != null) "At ${Labels.clock(pickedStop!!)}" else "Pick time…",
                            stopChoice == 2,
                            { showPicker = true },
                        )
                    }
                    Text("No alarms at or after the stop time. You can restart the plan any time.", color = c.muted, style = WardType.Body)
                }

                // Preview with real clock times
                Panel {
                    CapsLabel("Turn times")
                    Text("A late turn doesn't move them", color = c.muted, style = WardType.Body)
                    CardTimeline(TurnTimeline.preview(openedAt, currentOk, rotation, firstSlot, interval, stopAt, zone))
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().testTag(PRIMARY_TAG).windowInsetsPadding(WindowInsets.navigationBars).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
        ) {
            if (editing != null) {
                CapsLabel("Lying now")
                ChoiceRow(rotation.map { it.label }, rotation.indexOf(currentOk), { current = rotation[it] }, TvsSize.TouchLarge)
                PrimaryButton(
                    "Save changes",
                    onClick = { onSubmit(TurnPlanDraft(bed, interval, rotation, currentOk, firstSlot, stopAt)) },
                    enabled = validPlan,
                )
            } else {
                ChangeRow("First turn ${Labels.clock(firstSlot)}, then every ${Labels.duration(interval)}" + (stopAt?.let { " until ${Labels.clock(it)}" } ?: "")) {
                    options = true
                    scope.launch {
                        withFrameNanos { }
                        scroll.animateScrollTo(scroll.maxValue)
                    }
                }
                CapsLabel(if (bed.isBlank()) "Pick a bed first" else "$bed: lying now? Tap to start")
                PositionButtons(rotation, enabled = validPlan) { p ->
                    onSubmit(TurnPlanDraft(bed, interval, rotation, p, firstSlot, stopAt))
                }
            }
        }
    }

    if (showPicker) {
        val initial = Instant.ofEpochMilli(pickedStop ?: shiftEnd).atZone(zone)
        val state = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Stop turning at", style = WardType.Heading) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    pickedStop = Labels.nextOccurrence(state.hour, state.minute, System.currentTimeMillis(), zone)
                    stopChoice = 2
                    showPicker = false
                }) { Text("OK", style = WardType.Lead) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel", style = WardType.Lead) } },
        )
    }
}

/** Big position buttons: icon + name. Tapping one is the action. */
@Composable
private fun PositionButtons(positions: List<Position>, enabled: Boolean, onPick: (Position) -> Unit) {
    val c = Ward.c
    val perRow = if (largeFont()) 1 else 3
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
        positions.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                row.forEach { p ->
                    val fill = if (enabled) c.primary else c.bg
                    val ink = if (enabled) c.onPrimary else c.muted
                    Column(
                        Modifier.weight(1f).heightIn(min = TvsSize.TouchLarge)
                            .background(fill, TvsShape.Card)
                            .border(TvsSize.Edge, if (enabled) c.primaryEdge else c.muted, TvsShape.Card)
                            .wardClick(enabled = enabled) { onPick(p) }
                            .padding(vertical = TvsSpace.s6, horizontal = TvsSpace.s4),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PositionIcon(p, ink)
                        Text(p.label, color = ink, style = WardType.BodyBold)
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun OrderButton(label: String, enabled: Boolean, description: String, onClick: () -> Unit) {
    val c = Ward.c
    WardButton(
        label, onClick, Modifier.semantics { contentDescription = description },
        fill = c.card, content = if (enabled) c.ink else c.muted, edge = c.muted,
        minHeight = TvsSize.Touch, shape = TvsShape.Control, enabled = enabled,
    )
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> = toMutableList().also {
    val t = it[a]
    it[a] = it[b]
    it[b] = t
}

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.transfusionvs.data.Transfusion
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.ScheduleCalculator
import app.transfusionvs.schedule.ScheduleConfig
import app.transfusionvs.schedule.TimelineBuilder
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.HourlyPill
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import java.time.Instant
import java.time.ZoneId

private val DURATION_CHIPS = listOf(60, 120, 180, 240)
private val AGO_CHIPS = listOf(0, 5, 10, 15)
private const val MAX_BED_LABEL = 16

/**
 * Start transfusion, or Edit when [editing] is given. Start is enabled only after
 * "Baseline VS taken" is ticked. The preview shows the real clock times, like the card's example row.
 */
@Composable
fun StartScreen(vm: MainViewModel, editing: Transfusion?, onDone: () -> Unit, onBack: () -> Unit) {
    val openedAt = rememberSaveable { System.currentTimeMillis() }
    StartContent(
        editing = editing,
        limitMin = vm.limitMin,
        warnBeforeMin = vm.warnBeforeMin,
        openedAt = openedAt,
        onSubmit = { bed, startAt, duration ->
            if (editing == null) vm.start(bed, startAt, duration) else vm.edit(editing.id, bed, startAt, duration)
            onDone()
        },
        onBack = onBack,
    )
}

/** Stateless Start / Edit (rendered by the screenshot tests with a fixed [openedAt]). */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StartContent(
    editing: Transfusion?,
    limitMin: Int,
    warnBeforeMin: Int,
    openedAt: Long,
    onSubmit: (bed: String, startAt: Long, durationMin: Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var bed by rememberSaveable { mutableStateOf(editing?.bedLabel ?: "") }
    var startAt by rememberSaveable { mutableLongStateOf(editing?.startAt ?: ScheduleCalculator.truncateToMinute(openedAt)) }
    var agoChoice by rememberSaveable { mutableIntStateOf(if (editing == null) 0 else -1) } // -1 = picked time
    val initialDuration = editing?.plannedDurationMin
    var durationChoice by rememberSaveable {
        mutableIntStateOf(
            when {
                initialDuration == null -> 0 // nothing chosen yet
                initialDuration in DURATION_CHIPS -> initialDuration
                else -> -1 // custom
            },
        )
    }
    var customMinutes by rememberSaveable {
        mutableStateOf(if (initialDuration != null && initialDuration !in DURATION_CHIPS) initialDuration.toString() else "")
    }
    var baseline by rememberSaveable { mutableStateOf(editing != null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val duration: Int? = when (durationChoice) {
        0 -> null
        -1 -> customMinutes.toIntOrNull()?.takeIf { it in 10..600 }
        else -> durationChoice
    }
    val canSubmit = bed.isNotBlank() && duration != null && baseline

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(
            caps = if (editing == null) "New transfusion" else "Edit",
            title = if (editing == null) "Start transfusion" else "Edit ${editing.bedLabel}",
            onBack = onBack,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Bed — no patient identifiers
            Panel {
                CapsLabel("Bed", Tvs.Navy)
                OutlinedTextField(
                    value = bed,
                    onValueChange = { bed = it.take(MAX_BED_LABEL) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Tvs.Navy),
                    placeholder = { Text("e.g. Bed 12", fontSize = 22.sp) },
                    supportingText = { Text("Bed only — no names or MRN", color = Tvs.Red, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                if (editing == null) Text("Colour is assigned automatically.", color = Tvs.Muted, fontSize = 14.sp)
            }

            // Start time (T0): now by default, or earlier
            Panel {
                CapsLabel("Started at", Tvs.Navy)
                Text(Labels.clock(startAt), style = TimeFigures, color = Tvs.Navy, fontSize = 40.sp, fontWeight = FontWeight.Black)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AGO_CHIPS.forEach { ago ->
                        ChoiceChip(
                            text = if (ago == 0) "Now" else "$ago min ago",
                            selected = agoChoice == ago,
                            onClick = {
                                agoChoice = ago
                                startAt = ScheduleCalculator.truncateToMinute(System.currentTimeMillis()) - ago * MINUTE_MS
                            },
                        )
                    }
                    ChoiceChip("Pick time…", selected = agoChoice == -1, onClick = { showPicker = true })
                }
            }

            // Planned duration
            Panel {
                CapsLabel("Planned duration", Tvs.Navy)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATION_CHIPS.forEach { m ->
                        ChoiceChip(Labels.duration(m), selected = durationChoice == m, onClick = { durationChoice = m })
                    }
                    ChoiceChip("Custom", selected = durationChoice == -1, onClick = { durationChoice = -1 })
                }
                if (durationChoice == -1) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { v -> customMinutes = v.filter { it.isDigit() }.take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Minutes (10–600)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = customMinutes.isNotEmpty() && duration == null,
                    )
                }
            }

            // Baseline first — required before Start
            if (editing == null) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .background(if (baseline) Tvs.White else Tvs.Pink, RoundedCornerShape(16.dp))
                        .clickable { baseline = !baseline }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = baseline,
                        onCheckedChange = { baseline = it },
                        colors = CheckboxDefaults.colors(checkedColor = Tvs.Navy),
                    )
                    Text("Baseline VS taken", color = Tvs.Navy, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
            }

            // Preview with real clock times
            if (duration != null) {
                val config = ScheduleConfig(duration, limitMin, warnBeforeMin)
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CapsLabel("Time from start", Tvs.Navy)
                        Text(" · baseline first", color = Tvs.Muted, fontSize = 13.sp)
                    }
                    HourlyPill()
                    CardTimeline(TimelineBuilder.preview(startAt, config))
                    val warnAt = startAt + (config.limitMin - config.warnBeforeMin) * MINUTE_MS
                    Text(
                        "Then hourly until you tap End. Time-limit warning at ${Labels.clock(warnAt)} " +
                            "(${Labels.duration(config.limitMin)} limit).",
                        color = Tvs.Muted, fontSize = 14.sp,
                    )
                    if (startAt < openedAt - 15 * MINUTE_MS) {
                        Text(
                            "Checks that are already past will ring once, together, right after you start.",
                            color = Tvs.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp)) {
            Button(
                onClick = {
                    duration?.let { d ->
                        onSubmit(bed, startAt, d)
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
            ) {
                Text(
                    when {
                        editing != null -> "Save changes"
                        !baseline -> "Tick \"Baseline VS taken\" first"
                        else -> "Start transfusion"
                    },
                    fontSize = 19.sp, fontWeight = FontWeight.Black,
                )
            }
        }
    }

    if (showPicker) {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(startAt).atZone(zone)
        val state = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Transfusion started at") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    startAt = Labels.mostRecentPast(state.hour, state.minute, System.currentTimeMillis(), zone)
                    agoChoice = -1
                    showPicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        )
    }
}

/** Bed colour dot used in headers and lists. */
@Composable
fun BedDot(colorIndex: Int, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Box(Modifier.size(size).background(Tvs.bedColor(colorIndex), CircleShape))
}

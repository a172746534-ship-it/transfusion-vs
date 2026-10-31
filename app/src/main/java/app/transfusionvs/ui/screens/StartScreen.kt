package app.transfusionvs.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.data.Transfusion
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.ScheduleCalculator
import app.transfusionvs.schedule.ScheduleConfig
import app.transfusionvs.schedule.TimelineBuilder
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.BedGrid
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.CardTimeline
import app.transfusionvs.ui.components.ChangeRow
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.OTHER_BED
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.moreBelow
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

private val DURATION_CHIPS = listOf(60, 120, 180, 240)
private val AGO_CHIPS = listOf(0, 5, 10, 15)

/**
 * Start transfusion, or Edit when [editing] is given. Defaults instead of questions: started now, the
 * last duration used. Pick a bed, then "Baseline done · Start" (the confirmation is in the action).
 */
@Composable
fun StartScreen(vm: MainViewModel, editing: Transfusion?, onDone: () -> Unit, onBack: () -> Unit) {
    val openedAt = rememberSaveable { System.currentTimeMillis() }
    val beds by vm.wardBeds.collectAsStateWithLifecycle()
    val busy by vm.busyBeds.collectAsStateWithLifecycle()
    StartContent(
        editing = editing,
        beds = beds,
        busy = busy,
        defaultDurationMin = vm.lastDurationMin,
        limitMin = vm.limitMin,
        warnBeforeMin = vm.warnBeforeMin,
        openedAt = openedAt,
        onSubmit = { bed, startAt, duration ->
            if (editing == null) {
                vm.start(bed, startAt, duration)
                vm.lastDurationMin = duration
                if (bed !in beds) vm.addWardBeds(listOf(bed))
            } else {
                vm.edit(editing.id, bed, startAt, duration)
            }
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
    beds: List<String>,
    busy: Map<String, Int>,
    defaultDurationMin: Int,
    limitMin: Int,
    warnBeforeMin: Int,
    openedAt: Long,
    onSubmit: (bed: String, startAt: Long, durationMin: Int) -> Unit,
    onBack: () -> Unit,
    /** For screenshots: a bed already picked. */
    initialBed: String = "",
) {
    val context = LocalContext.current
    val c = Ward.c

    var picked by rememberSaveable { mutableStateOf(editing?.bedLabel ?: initialBed) }
    var otherText by rememberSaveable { mutableStateOf("") }
    val bed = if (picked == OTHER_BED) otherText.trim() else picked

    var startAt by rememberSaveable { mutableLongStateOf(editing?.startAt ?: ScheduleCalculator.truncateToMinute(openedAt)) }
    var agoChoice by rememberSaveable { mutableIntStateOf(if (editing == null) 0 else -1) } // -1 = picked time
    val initialDuration = editing?.plannedDurationMin ?: defaultDurationMin
    var durationChoice by rememberSaveable { mutableIntStateOf(if (initialDuration in DURATION_CHIPS) initialDuration else -1) } // -1 = custom
    var customMinutes by rememberSaveable { mutableStateOf(if (initialDuration !in DURATION_CHIPS) initialDuration.toString() else "") }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val duration: Int? = if (durationChoice == -1) customMinutes.toIntOrNull()?.takeIf { it in 10..600 } else durationChoice
    val canSubmit = bed.isNotBlank() && duration != null

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = if (editing == null) "New transfusion" else "Edit",
            title = if (editing == null) "Start transfusion" else "Edit ${editing.bedLabel}",
            onBack = onBack,
            icon = R.drawable.ic_drop,
        )
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        Column(
            Modifier.weight(1f).moreBelow(scroll.canScrollForward).verticalScroll(scroll).imePadding().padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            Panel {
                CapsLabel("Which bed?")
                BedGrid(
                    beds = (beds + listOfNotNull(editing?.bedLabel)).distinct(), busy = busy, selected = picked, otherText = otherText,
                    onPick = { picked = it }, onOtherText = { otherText = it },
                )
            }

            // Start time (T0): now by default, or earlier
            Panel {
                CapsLabel("Started at")
                Text(Labels.clock(startAt), style = WardType.Hero, color = c.ink)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
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
                CapsLabel("Planned duration")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
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
                        textStyle = WardType.Lead.copy(color = c.ink),
                        label = { Text("Minutes (10–600)", style = WardType.Body) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = customMinutes.isNotEmpty() && duration == null,
                    )
                }
            }

            // Preview with real clock times
            if (duration != null) {
                val config = ScheduleConfig(duration, limitMin, warnBeforeMin)
                Panel {
                    CapsLabel("Time from start")
                    Text("Baseline first · hourly = same minute as start", color = c.muted, style = WardType.Body)
                    CardTimeline(TimelineBuilder.preview(startAt, config))
                    val warnAt = startAt + (config.limitMin - config.warnBeforeMin) * MINUTE_MS
                    Text(
                        "Then hourly until you tap End. Time-limit warning at ${Labels.clock(warnAt)} " +
                            "(${Labels.duration(config.limitMin)} limit).",
                        color = c.muted, style = WardType.Body,
                    )
                    if (startAt < openedAt - 15 * MINUTE_MS) {
                        Text("Checks that are already past will ring once, together, right after you start.", color = c.ink, style = WardType.BodyBold)
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().testTag(PRIMARY_TAG).windowInsetsPadding(WindowInsets.navigationBars).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s6),
        ) {
            if (duration != null) {
                ChangeRow("Started ${Labels.clock(startAt)} · planned ${Labels.duration(duration)} · ends ${Labels.clock(startAt + duration * MINUTE_MS)}") {
                    scope.launch { scroll.animateScrollTo(scroll.maxValue) }
                }
            }
            PrimaryButton(
                when {
                    editing != null -> "Save changes"
                    bed.isBlank() -> "Pick a bed first"
                    else -> "Baseline done · Start"
                },
                onClick = { duration?.let { d -> onSubmit(bed, startAt, d) } },
                enabled = canSubmit,
            )
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
            title = { Text("Transfusion started at", style = WardType.Heading) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    startAt = Labels.mostRecentPast(state.hour, state.minute, System.currentTimeMillis(), zone)
                    agoChoice = -1
                    showPicker = false
                }) { Text("OK", style = WardType.Lead) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel", style = WardType.Lead) } },
        )
    }
}

package app.transfusionvs.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.CustomTimerWithSlots
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.schedule.CustomRules
import app.transfusionvs.schedule.CustomStatus
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PatientButton
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.StateText
import app.transfusionvs.ui.components.StatusPill
import app.transfusionvs.ui.components.UndoSlot
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.moreBelow
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import java.time.ZoneId

@Composable
fun CustomDetailScreen(vm: MainViewModel, id: Long, onBack: () -> Unit, onEdit: () -> Unit, onStopped: () -> Unit) {
    val flow = remember(id) { vm.customTimer(id) }
    val item by flow.collectAsStateWithLifecycle(initialValue = null)
    val now by rememberNow(15_000)
    CustomDetailContent(
        item = item, now = now, toleranceMin = vm.toleranceMin, missedTimeoutMin = vm.missedTimeoutMin,
        onBack = onBack, onEdit = onEdit,
        onStop = { timerId ->
            vm.stopCustom(timerId)
            onStopped()
        },
        onSetStop = vm::setCustomStop,
        onDone = vm::acknowledgeCustom,
    )
}

/**
 * One custom timer: the task, its next alarm, "End of my shift", the day's alarms, Edit and Stop.
 * Nothing about transfusions here: no steps, no End VS, no reaction bar.
 */
@Composable
fun CustomDetailContent(
    item: CustomTimerWithSlots?,
    now: Long,
    toleranceMin: Int,
    missedTimeoutMin: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStop: (Long) -> Unit,
    onSetStop: (Long, Long?) -> Unit,
    onDone: (List<Long>) -> Unit,
) {
    val c = Ward.c
    val t = item?.timer
    val running = t?.status == CustomTimerStatus.RUNNING
    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = if (t == null || running) "Custom timer running" else "Custom timer stopped",
            title = t?.bedLabel ?: "",
            titleIsKey = true,
            onBack = onBack,
            icon = R.drawable.ic_custom,
        )
        if (t == null || item == null) return@Column
        val open = item.slots.filter { it.status == CustomSlotStatus.RINGING }.minByOrNull { it.slotAt }

        val scroll = rememberScrollState()
        Column(
            Modifier.weight(1f).moreBelow(scroll.canScrollForward).verticalScroll(scroll).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            Panel {
                CapsLabel("What for")
                Text(t.name, color = c.ink, style = WardType.Title)
                val stops = t.stopAt?.takeIf { running }?.let { " · stops ${Labels.clock(it)}" } ?: ""
                Text("${Labels.every(t.intervalMin)} · started ${Labels.clock(t.startAt)}$stops", color = c.muted, style = WardType.Body)
            }

            if (running) {
                val view = BedViews.of(item, now, missedTimeoutMin)
                Panel {
                    val heading = if (open != null) "Now" else "Next"
                    // At large font the time goes under the state, so neither has to wrap.
                    if (largeFont()) {
                        CapsLabel(heading)
                        StateText(view)
                        view.time?.let { Text(Labels.clock(it), color = c.ink, style = WardType.Hero, softWrap = false) }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                CapsLabel(heading)
                                StateText(view)
                            }
                            view.time?.let { Text(Labels.clock(it), color = c.ink, style = WardType.Hero, softWrap = false) }
                        }
                    }
                }
                // Up here so they stay in view on a small phone. With an alarm open, Done is the action in the
                // thumb's place, and Edit and Stop share this row.
                if (open != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                        SecondaryButton("Edit", onEdit, Modifier.weight(1f))
                        SecondaryButton("Stop timer", { onStop(t.id) }, Modifier.weight(1f))
                    }
                } else {
                    SecondaryButton("Edit", onEdit)
                }
                // The one chip: a tap sets the stop time, a second clears it. The line below says which it is.
                Panel {
                    CapsLabel("Auto-stop")
                    val stop = t.stopAt
                    val shiftEnd = TurnGrid.endOfShift(now, ZoneId.systemDefault())
                    ChoiceChip(
                        "End of my shift (${Labels.clock(stop ?: shiftEnd)})",
                        selected = stop != null,
                        onClick = { onSetStop(t.id, if (stop != null) null else shiftEnd) },
                    )
                    Text(
                        if (stop != null) "On: no alarm at or after ${Labels.clock(stop)}. Tap again to turn it off." else "Off: it rings until you stop it.",
                        color = c.muted, style = WardType.Body,
                    )
                }
            } else {
                // When and how, for the next nurse.
                val at = t.stoppedAt
                val byStopTime = at != null && t.stopAt != null && at >= t.stopAt
                Panel {
                    Text(
                        when {
                            at == null -> "This timer has stopped."
                            byStopTime -> "Stopped at its stop time, ${Labels.clock(t.stopAt!!)}."
                            else -> "Stopped at ${Labels.clock(at)}."
                        },
                        color = c.ink, style = WardType.Lead,
                    )
                }
            }

            Panel {
                CapsLabel("Alarms today")
                val rows = item.slots
                    .filter { it.status != CustomSlotStatus.CANCELLED && it.slotAt >= now - CustomRules.KEEP_MS }
                    .sortedBy { it.slotAt }
                    .takeLast(ROWS)
                if (rows.isEmpty()) Text("None yet.", color = c.muted, style = WardType.Body)
                rows.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Labels.clock(s.slotAt), style = WardType.Time, color = c.ink, softWrap = false, modifier = Modifier.widthIn(min = TvsSize.TimeColumn))
                        Spacer(Modifier.weight(1f))
                        StatusPill(CustomStatus.of(s, now, toleranceMin, missedTimeoutMin))
                    }
                }
            }
        }

        // When Done empties the pinned place, Stop moves into it: a second tap meant for Done must not stop the timer.
        val openNow = open != null
        var stopAfter by remember { mutableLongStateOf(0L) }
        var wasOpen by remember { mutableStateOf(openNow) }
        LaunchedEffect(openNow) {
            if (wasOpen && !openNow) stopAfter = SystemClock.uptimeMillis() + STOP_GUARD_MS
            wasOpen = openNow
        }

        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = TvsSpace.s12).padding(top = TvsSpace.s8, bottom = TvsSpace.s8),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            UndoSlot()
            when {
                running && open != null -> PatientButton(Labels.clock(open.slotAt), Labels.DONE, { onDone(listOf(open.id)) }, Modifier.fillMaxWidth().testTag(PRIMARY_TAG))
                // Quiet on purpose: it cancels the alarms, and nothing else on this screen is asking to be pressed.
                running -> SecondaryButton(
                    "Stop timer",
                    { if (SystemClock.uptimeMillis() >= stopAfter) onStop(t.id) },
                    Modifier.fillMaxWidth().testTag(PRIMARY_TAG), TvsSize.TouchLarge,
                )
            }
        }
    }
}

/** The day's alarms shown: the latest few, then the next one. */
private const val ROWS = 8

/** How long Stop ignores taps after taking Done's place: the alarm screen's first second, the same way. */
private const val STOP_GUARD_MS = 1_000L

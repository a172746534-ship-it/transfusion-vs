package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.R
import app.transfusionvs.data.CustomDraft
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.schedule.CustomName
import app.transfusionvs.schedule.CustomRules
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.PatientInput
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.ChangeRow
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PatientField
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.TaskNameField
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.largeFont
import app.transfusionvs.ui.components.moreBelow
import app.transfusionvs.ui.components.wardClick
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** "Other" among the interval buttons. */
private const val OTHER = -1

@Composable
fun CustomStartScreen(vm: MainViewModel, editing: CustomTimer?, onStarted: (existingId: Long?) -> Unit, onBack: () -> Unit) {
    val customs by vm.customs.collectAsStateWithLifecycle()
    CustomStartContent(
        editing = editing,
        taken = customs.orEmpty().map { it.timer }.filter { it.id != editing?.id }.map { it.bedLabel to CustomName.key(it.name) }.toSet(),
        onSubmit = { draft ->
            if (editing == null) {
                vm.startCustom(draft, onStarted)
            } else {
                vm.editCustom(editing.id, draft)
                onStarted(null)
            }
        },
        onBack = onBack,
    )
}

/**
 * Start a custom timer, or Edit when [editing] is given. The task name, then the MRN, then an interval:
 * the interval buttons are the start buttons. A new timer always starts empty: nothing prefilled,
 * suggested or remembered, and no interval chosen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomStartContent(
    editing: CustomTimer?,
    /** Running timers' (MRN, name key), other than the one being edited: a rename onto one of them is refused. */
    taken: Set<Pair<String, String>> = emptySet(),
    onSubmit: (CustomDraft) -> Unit,
    onBack: () -> Unit,
    /** For screenshots: what is already typed, and the "Other" dialog open. */
    initialName: String = "",
    initialMrn: String = "",
    initialOther: Boolean = false,
) {
    val c = Ward.c
    var name by rememberSaveable { mutableStateOf(editing?.name ?: initialName) }
    // Editing keeps the patient unless asked: the title says who, and a slip here would move their alarms.
    var changingPatient by rememberSaveable { mutableStateOf(editing == null) }
    var mrn by rememberSaveable { mutableStateOf(initialMrn) }
    var interval by rememberSaveable { mutableIntStateOf(editing?.intervalMin ?: 0) }
    var showOther by rememberSaveable { mutableStateOf(initialOther) }
    val bed = if (changingPatient) mrn.takeIf { PatientInput.valid(it) } ?: "" else editing?.bedLabel.orEmpty()
    val nameOk = CustomName.valid(name)
    val duplicate = editing != null && nameOk && bed.isNotBlank() && (bed to CustomName.key(name)) in taken
    val ready = nameOk && bed.isNotBlank() && !duplicate
    val typing = WindowInsets.isImeVisible

    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = if (typing && largeFont()) "" else if (editing == null) "New custom timer" else "Edit",
            title = if (editing == null) "Start custom" else "Edit ${editing.bedLabel}",
            onBack = onBack,
            icon = R.drawable.ic_custom,
        )
        val scroll = rememberScrollState()
        Column(
            Modifier.weight(1f).moreBelow(scroll.canScrollForward).verticalScroll(scroll).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            Panel {
                CapsLabel("What for?")
                TaskNameField(name, { name = it }, autoFocus = editing == null)
            }
            Panel {
                CapsLabel("Which patient?")
                if (changingPatient) {
                    PatientField(mrn, autoFocus = false) { mrn = it }
                } else {
                    ChangeRow(editing?.bedLabel.orEmpty()) {
                        mrn = PatientInput.clean(editing?.bedLabel.orEmpty())
                        changingPatient = true
                    }
                }
            }
            if (duplicate) {
                Text("This patient already has a running timer with this name.", color = c.ink, style = WardType.BodyBold)
            }
        }

        Column(
            Modifier.fillMaxWidth().testTag(PRIMARY_TAG).windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s8),
        ) {
            if (editing == null) {
                // While the keyboard is open the fields keep the room; its Done brings the buttons back.
                if (!typing) {
                    // Once both are valid it says what a tap does: there is no separate Start button.
                    Text(if (ready) "How often? One tap starts it." else "Enter the task name and MRN first", color = c.ink, style = WardType.Lead)
                    IntervalButtons(selected = null, enabled = ready) { m ->
                        if (m == OTHER) showOther = true else onSubmit(CustomDraft(bed, name.trim(), m))
                    }
                }
            } else {
                if (!typing) {
                    IntervalButtons(selected = interval, enabled = true) { m -> if (m == OTHER) showOther = true else interval = m }
                }
                PrimaryButton("Save changes", onClick = { onSubmit(CustomDraft(bed, name.trim(), interval)) }, enabled = ready && interval > 0)
            }
        }
    }

    if (showOther) {
        OtherIntervalDialog(
            confirm = if (editing == null) "Start" else "Use",
            onConfirm = { m ->
                showOther = false
                if (editing == null) onSubmit(CustomDraft(bed, name.trim(), m)) else interval = m
            },
            onDismiss = { showOther = false },
        )
    }
}

/**
 * 15 min … 12 h and Other, three across. On a new timer each one starts it; on an edit they choose,
 * with the current interval marked.
 */
@Composable
private fun IntervalButtons(selected: Int?, enabled: Boolean, onPick: (Int) -> Unit) {
    val c = Ward.c
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
        (CustomRules.INTERVALS + OTHER).chunked(3).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                row.forEach { m ->
                    val choosing = selected != null
                    val on = choosing && (m == selected || (m == OTHER && selected!! > 0 && selected !in CustomRules.INTERVALS))
                    val filled = enabled && (!choosing || on)
                    val fill = if (filled) c.primary else if (enabled) c.card else c.bg
                    val ink = if (filled) c.onPrimary else if (enabled) c.ink else c.muted
                    val label = if (m == OTHER) (if (on) Labels.duration(selected!!) else "Other") else Labels.duration(m)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = TvsSize.TouchLarge)
                            .background(fill, TvsShape.Card)
                            .border(TvsSize.Edge, if (filled) c.primaryEdge else c.muted, TvsShape.Card)
                            .semantics { if (choosing) this.selected = on }
                            .wardClick(enabled = enabled, role = if (choosing) Role.RadioButton else Role.Button) { onPick(m) }
                            .padding(horizontal = TvsSpace.s4, vertical = TvsSpace.s6),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (on) "✓ $label" else label, color = ink, style = WardType.BodyBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** Any interval from 15 min to 24 h, in 5-minute steps. It opens at 15 min every time. */
@Composable
private fun OtherIntervalDialog(confirm: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Ward.c
    var minutes by rememberSaveable { mutableIntStateOf(CustomRules.MIN_INTERVAL) }
    fun change(by: Int) {
        minutes = (minutes + by).coerceIn(CustomRules.MIN_INTERVAL, CustomRules.MAX_INTERVAL)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Every", color = c.ink, style = WardType.Heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s12), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Labels.duration(minutes), color = c.ink, style = WardType.Hero)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    Step("− 1 h", Modifier.weight(1f), enabled = minutes > CustomRules.MIN_INTERVAL) { change(-60) }
                    Step("+ 1 h", Modifier.weight(1f), enabled = minutes < CustomRules.MAX_INTERVAL) { change(60) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    Step("− ${CustomRules.OTHER_STEP} min", Modifier.weight(1f), enabled = minutes > CustomRules.MIN_INTERVAL) { change(-CustomRules.OTHER_STEP) }
                    Step("+ ${CustomRules.OTHER_STEP} min", Modifier.weight(1f), enabled = minutes < CustomRules.MAX_INTERVAL) { change(CustomRules.OTHER_STEP) }
                }
                Text("From 15 min to 24 h.", color = c.muted, style = WardType.Body)
            }
        },
        confirmButton = {
            WardButton(confirm, { onConfirm(minutes) }, Modifier.heightIn(min = TvsSize.Touch), fill = c.primary, content = c.onPrimary, minHeight = TvsSize.Touch)
        },
        dismissButton = {
            WardButton("Cancel", onDismiss, Modifier.heightIn(min = TvsSize.Touch), fill = c.card, content = c.ink, edge = c.muted, minHeight = TvsSize.Touch)
        },
    )
}

/** At the limit it looks and acts disabled, like the interval buttons before the fields are filled. */
@Composable
private fun Step(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val c = Ward.c
    WardButton(
        text, onClick, modifier, fill = if (enabled) c.card else c.bg, content = if (enabled) c.ink else c.muted, edge = c.muted,
        minHeight = TvsSize.Touch, shape = TvsShape.Control, style = WardType.BodyBold, enabled = enabled,
    )
}

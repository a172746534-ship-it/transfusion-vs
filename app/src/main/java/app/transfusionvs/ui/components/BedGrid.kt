package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import app.transfusionvs.R
import app.transfusionvs.schedule.PatientInput
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** The value for "+ New MRN" while it is being typed. */
const val OTHER_BED = "\u0000other"

/**
 * Pick a patient with one tap: everyone seen lately, those with running timers first, each with a drop
 * or turn icon. A new patient's MRN is typed once, digits only.
 */
@Composable
fun BedGrid(
    beds: List<String>,
    busy: Map<String, Int>,
    selected: String,
    otherText: String,
    onPick: (String) -> Unit,
    onOtherText: (String) -> Unit,
) {
    val c = Ward.c
    // As many across as the longest MRN allows, so the last digits are never cut off.
    val longest = beds.maxOfOrNull { it.length } ?: 0
    val columns = when {
        largeFont() || longest > 12 -> 1
        longest > 7 -> 2
        else -> 3
    }
    val ordered = PatientInput.sorted(beds.filter { it in busy }) + PatientInput.sorted(beds.filter { it !in busy })
    val typing = selected == OTHER_BED || (selected.isBlank() && beds.isEmpty())
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
        if (beds.isNotEmpty()) {
            (ordered + OTHER_BED).chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    row.forEach { bed ->
                        val on = bed == selected
                        Box(
                            Modifier.weight(1f).fillMaxHeight().heightIn(min = TvsSize.TouchLarge)
                                .background(if (on) c.primary else c.card, TvsShape.Control)
                                .border(TvsSize.Edge, if (on) c.primaryEdge else c.muted, TvsShape.Control)
                                .semantics { this.selected = on }
                                .wardClick(role = Role.RadioButton) { onPick(bed) }
                                .padding(horizontal = TvsSpace.s4, vertical = TvsSpace.s4),
                        ) {
                            val ink = if (on) c.onPrimary else c.ink
                            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                val icon = busy[bed]
                                if (icon != null) {
                                    Icon(painterResource(icon), contentDescription = "Running", tint = ink, modifier = Modifier.size(TvsSize.IconSmall))
                                    Spacer(Modifier.width(TvsSpace.s2))
                                }
                                if (bed == OTHER_BED) {
                                    Text("+ New MRN", color = ink, style = WardType.BodyBold, textAlign = TextAlign.Center)
                                } else {
                                    LabelText(bed, ink, WardType.Lead)
                                }
                            }
                            // Selected also carries a mark, so it never rests on colour alone, and the cell keeps its size.
                            if (on) {
                                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = ink, modifier = Modifier.align(Alignment.TopEnd).size(TvsSize.IconSmall))
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (typing) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            OutlinedTextField(
                value = otherText,
                onValueChange = { onOtherText(PatientInput.clean(it)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                singleLine = true,
                textStyle = WardType.Heading.copy(color = c.ink),
                label = { Text("Patient's MRN", style = WardType.Body) },
                supportingText = { Text("${PatientInput.MIN_DIGITS} to ${PatientInput.MAX_DIGITS} digits. No names.", color = c.ink, style = WardType.Body) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

/** A recent patient on the Settings screen. Tap to remove it from this phone. */
@Composable
fun BedChip(label: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ward.c
    Row(
        modifier.heightIn(min = TvsSize.Touch)
            .background(c.card, TvsShape.Control)
            .border(TvsSize.Edge, c.muted, TvsShape.Control)
            .wardClick(onClick = onRemove)
            .padding(horizontal = TvsSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.ink, style = WardType.BodyBold)
        Spacer(Modifier.width(TvsSpace.s6))
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = "Remove", tint = c.muted, modifier = Modifier.size(TvsSize.IconSmall))
        }
    }
}

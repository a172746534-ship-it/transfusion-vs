package app.transfusionvs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.transfusionvs.R
import app.transfusionvs.schedule.BedInput
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** The value for "+ Other bed" while it is being typed. */
const val OTHER_BED = "\u0000other"

/**
 * Pick a bed with one tap. Beds with running timers come first and carry a drop or turn icon.
 * "+ Other bed" with typing stays as a rare fallback.
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
    val columns = if (largeFont()) 2 else 4
    val ordered = BedInput.sorted(beds.filter { it in busy }) + BedInput.sorted(beds.filter { it !in busy })
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
        (ordered + OTHER_BED).chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                row.forEach { bed ->
                    val on = bed == selected
                    Row(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = TvsSize.TouchLarge)
                            .background(if (on) c.primary else c.card, TvsShape.Control)
                            .border(TvsSize.Edge, if (on) c.primaryEdge else c.muted, TvsShape.Control)
                            .wardClick(role = Role.RadioButton) { onPick(bed) }
                            .padding(horizontal = TvsSpace.s4, vertical = TvsSpace.s4),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val ink = if (on) c.onPrimary else c.ink
                        val icon = busy[bed]
                        if (icon != null) {
                            Icon(painterResource(icon), contentDescription = "Running", tint = ink, modifier = Modifier.size(TvsSize.IconSmall))
                            Spacer(Modifier.width(TvsSpace.s2))
                        }
                        Text(
                            when {
                                bed == OTHER_BED -> "+ Other bed"
                                on -> "✓ $bed"
                                else -> bed
                            },
                            color = ink, style = WardType.BodyBold, textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (selected == OTHER_BED) {
            OutlinedTextField(
                value = otherText,
                onValueChange = { onOtherText(it.take(BedInput.MAX_LABEL)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = WardType.Lead.copy(color = c.ink),
                placeholder = { Text("e.g. Bed 12", color = c.muted, style = WardType.Lead) },
                supportingText = { Text("Bed only — no names or MRN", color = c.ink, style = WardType.BodyBold) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
    }
}

/** "No beds yet" hint plus chips with × for the Settings and Setup screens. */
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

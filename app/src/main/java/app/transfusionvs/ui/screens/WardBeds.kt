package app.transfusionvs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.transfusionvs.schedule.BedInput
import app.transfusionvs.ui.components.BedChip
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** Enter the ward's beds once; every flow then picks a bed with one tap. Bed numbers, never names. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WardBedsPanel(beds: List<String>, onAdd: (List<String>) -> Unit, onRemove: (String) -> Unit) {
    val c = Ward.c
    var text by rememberSaveable { mutableStateOf("") }
    Panel {
        CapsLabel("Ward beds")
        Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            Text(
                if (beds.isEmpty()) "Enter your beds once. Then you pick a bed with one tap." else "Tap a bed to remove it.",
                color = c.muted, style = WardType.Body,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                BedInput.sorted(beds).forEach { bed -> BedChip(bed, { onRemove(bed) }) }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = WardType.Lead.copy(color = c.ink),
                label = { Text("Beds, e.g. 1-20, 25, ICU 3", style = WardType.Body) },
                supportingText = { Text("Bed only — no names or MRN", color = c.ink, style = WardType.Body) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            val parsed = BedInput.parse(text)
            if (parsed.isNotEmpty()) {
                SecondaryButton(
                    "Add ${parsed.size} bed${if (parsed.size > 1) "s" else ""}",
                    {
                        onAdd(parsed)
                        text = ""
                    },
                )
            }
        }
    }
}

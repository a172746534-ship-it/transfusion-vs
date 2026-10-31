package app.transfusionvs.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.transfusionvs.schedule.PatientInput
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** The patient's MRN, on the number pad, ready to type as soon as the screen opens. Digits only, so a name can't get in. */
@Composable
fun PatientField(value: String, onValue: (String) -> Unit) {
    val c = Ward.c
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(PatientInput.clean(it)) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        singleLine = true,
        textStyle = WardType.CardBed.copy(color = c.ink),
        label = { Text("Patient's MRN", style = WardType.Body) },
        supportingText = { Text("${PatientInput.MIN_DIGITS} to ${PatientInput.MAX_DIGITS} digits. No names.", color = c.ink, style = WardType.Body) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        colors = wardFieldColors(),
    )
}

/** As many across as the longest MRN allows, so a number is never cut off or broken in two. */
@Composable
fun patientColumns(labels: List<String>): Int {
    val longest = labels.maxOfOrNull { it.length } ?: 0
    return when {
        largeFont() || longest > 12 -> 1
        longest > 7 -> 2
        else -> 3
    }
}

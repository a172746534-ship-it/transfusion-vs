package app.transfusionvs.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.transfusionvs.schedule.CustomName
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/**
 * A custom timer's task name, typed: one line, 30 characters, no suggestions of the app's own.
 * The keyboard's Next moves on to the MRN.
 */
@Composable
fun TaskNameField(value: String, onValue: (String) -> Unit, autoFocus: Boolean) {
    val c = Ward.c
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val count = value.trim().let { it.codePointCount(0, it.length) }
    // At large font sizes the label can't fit on the box's border: it goes above the box, word for word.
    val above = largeFont()
    if (above) Text(LABEL, color = c.ink, style = WardType.Body)
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(CustomName.clean(it)) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus).let { if (above) it.semantics { contentDescription = LABEL } else it },
        // One line of text (line breaks are removed as typed), shown on as many as it needs: a long name is never hidden.
        singleLine = false,
        maxLines = 3,
        textStyle = WardType.Lead.copy(color = c.ink),
        label = if (above) null else { { Text(LABEL, style = WardType.Body) } },
        supportingText = { Text("$count/${CustomName.MAX}", color = c.ink, style = WardType.Body) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        colors = wardFieldColors(),
    )
}

private const val LABEL = "Task name — no patient names"

/**
 * A task name shown large: it shrinks to stay within two lines, never below body size, and takes a
 * third line only when even that can't hold it. It is never cut off.
 */
@Composable
fun TaskNameText(name: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    var lines by remember(name) { mutableIntStateOf(2) }
    BasicText(
        name, modifier, style = style.copy(color = color), maxLines = lines,
        autoSize = TextAutoSize.StepBased(minFontSize = WardType.Body.fontSize, maxFontSize = style.fontSize),
        onTextLayout = { if (it.hasVisualOverflow && lines < 3) lines = 3 },
    )
}

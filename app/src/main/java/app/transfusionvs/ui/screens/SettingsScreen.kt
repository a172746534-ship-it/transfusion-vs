package app.transfusionvs.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import app.transfusionvs.app
import app.transfusionvs.data.Settings
import app.transfusionvs.schedule.Labels
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import kotlinx.coroutines.launch
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

/** A snapshot of every setting shown on the screen. */
data class SettingsValues(
    val soundOn: Boolean,
    val soundUri: String?,
    val snoozeMin: Int,
    val missedTimeoutMin: Int,
    val toleranceMin: Int,
    val preAlertMin: Int,
    val limitMin: Int,
    val warnBeforeMin: Int,
) {
    companion object {
        fun from(s: Settings) = SettingsValues(
            s.soundOn, s.alarmSoundUri, s.snoozeMin, s.missedTimeoutMin, s.toleranceMin, s.preAlertMin, s.limitMin, s.warnBeforeMin,
        )
    }
}

/** Every change is saved at once and re-arms all alarms. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onSetup: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val s = app.settings
    var values by remember { mutableStateOf(SettingsValues.from(s)) }

    fun save(v: SettingsValues) {
        s.soundOn = v.soundOn
        s.alarmSoundUri = v.soundUri
        s.snoozeMin = v.snoozeMin
        s.missedTimeoutMin = v.missedTimeoutMin
        s.toleranceMin = v.toleranceMin
        s.preAlertMin = v.preAlertMin
        s.limitMin = v.limitMin
        s.warnBeforeMin = v.warnBeforeMin
        values = SettingsValues.from(s)
        app.scope.launch { app.engine.rearm(canStartService = true) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            // The phone's default alarm sound is stored as "no choice", so it follows the phone's setting.
            val value = picked?.takeIf { it != android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI }?.toString()
            save(values.copy(soundUri = value))
        }
    }

    SettingsContent(
        values = values,
        soundTitle = soundTitle(context, values.soundUri),
        onChange = ::save,
        onPickSound = {
            val existing = values.soundUri?.let(Uri::parse) ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
            try {
                picker.launch(intent)
            } catch (e: ActivityNotFoundException) {
                // No sound picker on this phone: the default alarm sound stays.
            }
        },
        onBack = onBack,
        onSetup = onSetup,
    )
}

/** Stateless Settings (rendered by the screenshot tests). */
@Composable
fun SettingsContent(
    values: SettingsValues,
    soundTitle: String,
    onChange: (SettingsValues) -> Unit,
    onPickSound: () -> Unit,
    onBack: () -> Unit,
    onSetup: () -> Unit,
) {
    val v = values
    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(caps = "Settings", title = "Alarm settings", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(TvsSpace.s16),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            Panel {
                CapsLabel("Alarm", Tvs.Navy)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    ChoiceChip("Sound + vibrate", v.soundOn, { onChange(v.copy(soundOn = true)) }, Modifier.weight(1f))
                    ChoiceChip("Vibrate only", !v.soundOn, { onChange(v.copy(soundOn = false)) }, Modifier.weight(1f))
                }
                if (!v.soundOn) {
                    Text(
                        "Night mode: alarms vibrate but make NO sound. Keep the phone on you.",
                        color = Tvs.Red, fontSize = TvsFont.Caption, fontWeight = FontWeight.Bold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Alarm sound", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                        Text(soundTitle, color = Tvs.Muted, fontSize = TvsFont.Caption)
                    }
                    OutlinedButton(
                        onClick = onPickSound,
                        modifier = Modifier.heightIn(min = TvsSize.Touch),
                        shape = TvsShape.Control,
                    ) { Text("Change", fontWeight = FontWeight.Bold) }
                }
                Text("Snooze length", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    Settings.SNOOZE_RANGE.forEach { m ->
                        ChoiceChip("$m min", v.snoozeMin == m, { onChange(v.copy(snoozeMin = m)) }, Modifier.weight(1f))
                    }
                }
                Stepper(
                    title = "Missed after",
                    hint = "Ringing stops and the check is marked missed (an Overdue reminder stays).",
                    value = "${v.missedTimeoutMin} min",
                    onMinus = { if (v.missedTimeoutMin > Settings.MISSED_RANGE.first) onChange(v.copy(missedTimeoutMin = v.missedTimeoutMin - 1)) },
                    onPlus = { if (v.missedTimeoutMin < Settings.MISSED_RANGE.last) onChange(v.copy(missedTimeoutMin = v.missedTimeoutMin + 1)) },
                )
            }

            Panel {
                CapsLabel("Checkpoints", Tvs.Navy)
                Stepper(
                    title = "On-time tolerance",
                    hint = "Documented within this many minutes of the scheduled time counts as on time.",
                    value = "±${v.toleranceMin} min",
                    onMinus = { if (v.toleranceMin > Settings.TOLERANCE_RANGE.first) onChange(v.copy(toleranceMin = v.toleranceMin - 1)) },
                    onPlus = { if (v.toleranceMin < Settings.TOLERANCE_RANGE.last) onChange(v.copy(toleranceMin = v.toleranceMin + 1)) },
                )
                Text("Pre-alert (gentle notification + short vibration)", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    Settings.PRE_ALERT_CHOICES.forEach { m ->
                        ChoiceChip(if (m == 0) "Off" else "$m min", v.preAlertMin == m, { onChange(v.copy(preAlertMin = m)) }, Modifier.weight(1f))
                    }
                }
            }

            Panel {
                CapsLabel("Time limit", Tvs.Navy)
                Stepper(
                    title = "Maximum duration",
                    hint = null,
                    value = Labels.duration(v.limitMin),
                    onMinus = {
                        if (v.limitMin > Settings.LIMIT_RANGE.first) {
                            val limit = v.limitMin - 15
                            onChange(v.copy(limitMin = limit, warnBeforeMin = minOf(v.warnBeforeMin, limit - 5)))
                        }
                    },
                    onPlus = { if (v.limitMin < Settings.LIMIT_RANGE.last) onChange(v.copy(limitMin = v.limitMin + 15)) },
                )
                Stepper(
                    title = "Warn before the limit",
                    hint = "An alarm this long before the maximum duration.",
                    value = "${v.warnBeforeMin} min",
                    onMinus = { if (v.warnBeforeMin > Settings.WARN_RANGE.first) onChange(v.copy(warnBeforeMin = v.warnBeforeMin - 5)) },
                    onPlus = {
                        if (v.warnBeforeMin < Settings.WARN_RANGE.last && v.warnBeforeMin + 5 < v.limitMin) {
                            onChange(v.copy(warnBeforeMin = v.warnBeforeMin + 5))
                        }
                    },
                )
            }

            Button(
                onClick = onSetup,
                modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.Touch),
                shape = TvsShape.Card,
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
            ) { Text("Setup check & test alarm", fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Black) }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun Stepper(title: String, hint: String?, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StepButton("−", onMinus)
            Text(
                value, style = TimeFigures, color = Tvs.Navy, fontSize = TvsFont.Lead, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = TvsSize.StepperValue),
            )
            StepButton("+", onPlus)
        }
        if (hint != null) Text(hint, color = Tvs.Muted, fontSize = TvsFont.Label)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(TvsSize.Touch),
        shape = TvsShape.Control,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) { Text(label, fontSize = TvsFont.Title, fontWeight = FontWeight.Black, color = Tvs.Navy) }
}

private fun soundTitle(context: Context, uri: String?): String {
    if (uri == null) return "Phone's default alarm sound"
    return runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull() ?: "Chosen sound"
}

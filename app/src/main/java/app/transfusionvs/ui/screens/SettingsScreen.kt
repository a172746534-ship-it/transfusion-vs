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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.IntentCompat
import app.transfusionvs.app
import app.transfusionvs.data.Settings
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.NightRule
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.ChoiceRow
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.components.moreBelow
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import kotlinx.coroutines.launch

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
    val turnIntervalMin: Int = 120,
    val turnWindowMin: Int = 15,
    val turnSoundUri: String? = null,
    val themeMode: Int = NightRule.AUTO_CLOCK,
    val nightStartHour: Int = 20,
    val nightEndHour: Int = 8,
) {
    companion object {
        fun from(s: Settings) = SettingsValues(
            s.soundOn, s.alarmSoundUri, s.snoozeMin, s.missedTimeoutMin, s.toleranceMin, s.preAlertMin, s.limitMin, s.warnBeforeMin,
            s.turnIntervalMin, s.turnWindowMin, s.turnSoundUri, s.themeMode, s.nightStartHour, s.nightEndHour,
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
        s.turnIntervalMin = v.turnIntervalMin
        s.turnWindowMin = v.turnWindowMin
        s.turnSoundUri = v.turnSoundUri
        s.themeMode = v.themeMode
        s.nightStartHour = v.nightStartHour
        s.nightEndHour = v.nightEndHour
        values = SettingsValues.from(s)
        app.themeVersion.intValue++
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
    // Turning rounds have their own sound; "default" there means the bundled turning chime.
    val turnPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            val value = picked?.takeIf { it != android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI }?.toString()
            save(values.copy(turnSoundUri = value))
        }
    }

    SettingsContent(
        values = values,
        soundTitle = soundTitle(context, values.soundUri),
        turnSoundTitle = values.turnSoundUri?.let { soundTitle(context, it) } ?: "Turning chime (built in)",
        onPickTurnSound = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, values.turnSoundUri?.let(Uri::parse))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Turning alarm sound")
            try {
                turnPicker.launch(intent)
            } catch (e: ActivityNotFoundException) {
                // No sound picker on this phone: the turning chime stays.
            }
        },
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    values: SettingsValues,
    soundTitle: String,
    turnSoundTitle: String = "Turning chime (built in)",
    onChange: (SettingsValues) -> Unit,
    onPickSound: () -> Unit,
    onPickTurnSound: () -> Unit = {},
    onBack: () -> Unit,
    onSetup: () -> Unit,
) {
    val v = values
    val c = Ward.c
    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(caps = "Settings", title = "Alarm settings", onBack = onBack)
        val scroll = rememberScrollState()
        Column(
            Modifier.weight(1f).moreBelow(scroll.canScrollForward).verticalScroll(scroll).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            Panel {
                CapsLabel("Night theme")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    listOf(
                        NightRule.AUTO_CLOCK to "By the clock",
                        NightRule.FOLLOW_SYSTEM to "Follow the phone",
                        NightRule.ALWAYS_DAY to "Always day",
                        NightRule.ALWAYS_NIGHT to "Always night",
                    ).forEach { (mode, label) ->
                        ChoiceChip(label, v.themeMode == mode, { onChange(v.copy(themeMode = mode)) })
                    }
                }
                if (v.themeMode == NightRule.AUTO_CLOCK) {
                    Stepper(
                        title = "Night starts",
                        hint = null,
                        value = "${v.nightStartHour.toString().padStart(2, '0')}:00",
                        onMinus = { onChange(v.copy(nightStartHour = Math.floorMod(v.nightStartHour - 1, 24))) },
                        onPlus = { onChange(v.copy(nightStartHour = (v.nightStartHour + 1) % 24)) },
                    )
                    Stepper(
                        title = "Night ends",
                        hint = null,
                        value = "${v.nightEndHour.toString().padStart(2, '0')}:00",
                        onMinus = { onChange(v.copy(nightEndHour = Math.floorMod(v.nightEndHour - 1, 24))) },
                        onPlus = { onChange(v.copy(nightEndHour = (v.nightEndHour + 1) % 24)) },
                    )
                }
            }

            Panel {
                CapsLabel("Alarm")
                ChoiceRow(listOf("Sound + vibrate", "Vibrate only"), if (v.soundOn) 0 else 1, { onChange(v.copy(soundOn = it == 0)) })
                if (!v.soundOn) {
                    Text("Night mode: alarms vibrate but make NO sound. Keep the phone on you.", color = c.ink, style = WardType.BodyBold)
                }
                SoundRow("Transfusion alarm sound", soundTitle, onPickSound)
                Text("Snooze length", color = c.ink, style = WardType.BodyBold)
                val snoozes = Settings.SNOOZE_RANGE.toList()
                ChoiceRow(snoozes.map { "$it min" }, snoozes.indexOf(v.snoozeMin), { onChange(v.copy(snoozeMin = snoozes[it])) })
                Stepper(
                    title = "Missed after",
                    hint = "Ringing stops and the check is marked missed (an Overdue reminder stays).",
                    value = "${v.missedTimeoutMin} min",
                    onMinus = { if (v.missedTimeoutMin > Settings.MISSED_RANGE.first) onChange(v.copy(missedTimeoutMin = v.missedTimeoutMin - 1)) },
                    onPlus = { if (v.missedTimeoutMin < Settings.MISSED_RANGE.last) onChange(v.copy(missedTimeoutMin = v.missedTimeoutMin + 1)) },
                )
            }

            Panel {
                CapsLabel("Checkpoints")
                Stepper(
                    title = "On-time tolerance",
                    hint = "Documented within this many minutes of the scheduled time counts as on time.",
                    value = "±${v.toleranceMin} min",
                    onMinus = { if (v.toleranceMin > Settings.TOLERANCE_RANGE.first) onChange(v.copy(toleranceMin = v.toleranceMin - 1)) },
                    onPlus = { if (v.toleranceMin < Settings.TOLERANCE_RANGE.last) onChange(v.copy(toleranceMin = v.toleranceMin + 1)) },
                )
                Text("Pre-alert (gentle notification + short vibration)", color = c.ink, style = WardType.BodyBold)
                ChoiceRow(
                    Settings.PRE_ALERT_CHOICES.map { if (it == 0) "Off" else "$it min" },
                    Settings.PRE_ALERT_CHOICES.indexOf(v.preAlertMin),
                    { onChange(v.copy(preAlertMin = Settings.PRE_ALERT_CHOICES[it])) },
                )
            }

            Panel {
                CapsLabel("Time limit")
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

            Panel {
                CapsLabel("Repositioning")
                Text("Ward interval for new plans", color = c.ink, style = WardType.BodyBold)
                ChoiceRow(
                    Settings.TURN_INTERVALS.map { Labels.duration(it) },
                    Settings.TURN_INTERVALS.indexOf(v.turnIntervalMin),
                    { onChange(v.copy(turnIntervalMin = Settings.TURN_INTERVALS[it])) },
                )
                Stepper(
                    title = "On-time window",
                    hint = "A turn within this many minutes of its slot counts as on time.",
                    value = "±${v.turnWindowMin} min",
                    onMinus = { if (v.turnWindowMin > Settings.TURN_WINDOW_RANGE.first) onChange(v.copy(turnWindowMin = v.turnWindowMin - 5)) },
                    onPlus = { if (v.turnWindowMin < Settings.TURN_WINDOW_RANGE.last) onChange(v.copy(turnWindowMin = v.turnWindowMin + 5)) },
                )
                SoundRow("Turning alarm sound", turnSoundTitle, onPickTurnSound)
            }
        }
        Box(Modifier.fillMaxWidth().testTag(PRIMARY_TAG).windowInsetsPadding(WindowInsets.navigationBars).padding(TvsSpace.s12)) {
            PrimaryButton("Setup check & test alarm", onSetup)
        }
    }
}

@Composable
private fun SoundRow(title: String, sound: String, onChange: () -> Unit) {
    val c = Ward.c
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
        Text(title, color = c.ink, style = WardType.BodyBold)
        Text(sound, color = c.muted, style = WardType.Body)
        SecondaryButton("Change $title".replace(" alarm sound", " sound"), onChange)
    }
}

@Composable
private fun Stepper(title: String, hint: String?, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = Ward.c
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
        Text(title, color = c.ink, style = WardType.BodyBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", "$title: less", onMinus)
            Spacer(Modifier.width(TvsSpace.s8))
            Text(
                value, style = WardType.Time, color = c.ink,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f).widthIn(min = TvsSize.Stepper),
            )
            Spacer(Modifier.width(TvsSpace.s8))
            StepButton("+", "$title: more", onPlus)
        }
        if (hint != null) Text(hint, color = c.muted, style = WardType.Body)
    }
}

@Composable
private fun StepButton(label: String, description: String, onClick: () -> Unit) {
    val c = Ward.c
    WardButton(
        label, onClick, Modifier.semantics { contentDescription = description },
        fill = c.card, content = c.ink, edge = c.muted, minHeight = TvsSize.Touch, shape = TvsShape.Control,
    )
}

private fun soundTitle(context: Context, uri: String?): String {
    if (uri == null) return "Phone's default alarm sound"
    return runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull() ?: "Chosen sound"
}

package app.transfusionvs.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.BuildConfig
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.app
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.setup.CheckItem
import app.transfusionvs.setup.OemGuide
import app.transfusionvs.setup.OemGuides
import app.transfusionvs.setup.OemStep
import app.transfusionvs.setup.SetupChecks
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.Tvs
import kotlinx.coroutines.launch
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsFont

/** Setup check: live permission checklist, per-brand steps, and real test alarms. */
@Composable
fun SetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.app

    var checks by remember { mutableStateOf(SetupChecks.evaluate(context)) }
    LifecycleResumeEffect(Unit) {
        checks = SetupChecks.evaluate(context) // live: re-checked every time you come back from Settings
        onPauseOrDispose { }
    }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checks = SetupChecks.evaluate(context)
    }
    val guide = remember { OemGuides.forThisPhone(context) }
    val tests by app.db.dao().observeTests().collectAsStateWithLifecycle(initialValue = emptyList())
    val ringing by app.db.dao().observeRinging().collectAsStateWithLifecycle(initialValue = emptyList())
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    fun fix(item: CheckItem) {
        if (item.askNotificationPermission && !askedForNotifications) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            SetupChecks.open(context, item.fix)
        }
    }

    fun test(delayMs: Long, hint: String) {
        val at = System.currentTimeMillis() + delayMs
        app.scope.launch { app.engine.scheduleTest(delayMs) }
        message = "Test alarm set for ${Labels.clock(at)}. $hint"
    }

    SetupContent(
        checks = checks,
        guide = guide,
        tests = tests,
        ringing = ringing.isNotEmpty(),
        message = message,
        onBack = onBack,
        onFix = ::fix,
        onOpenStep = { SetupChecks.open(context, it.intents) },
        onTest = ::test,
        onCancelTest = { id -> app.scope.launch { app.engine.cancelTest(id) } },
        versionName = BuildConfig.VERSION_NAME,
        onOpenAlarm = {
            context.startActivity(Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    )
}

/** Stateless Setup check (rendered by the screenshot tests with fake data). */
@Composable
fun SetupContent(
    checks: List<CheckItem>,
    guide: OemGuide,
    tests: List<CheckpointWithBed>,
    ringing: Boolean,
    message: String?,
    onBack: () -> Unit,
    onFix: (CheckItem) -> Unit,
    onOpenStep: (OemStep) -> Unit,
    onTest: (delayMs: Long, hint: String) -> Unit,
    onCancelTest: (Long) -> Unit,
    onOpenAlarm: () -> Unit,
    versionName: String,
) {
    Column(Modifier.fillMaxSize().background(Tvs.Panel).verticalScroll(rememberScrollState())) {
        TopBar(
            caps = "Setup check",
            title = "Make sure alarms can't be missed",
            subtitle = "${checks.count { it.ok }} of ${checks.size} ready",
            onBack = onBack,
        )

        Column(
            Modifier.padding(TvsSpace.s16).windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s16),
        ) {
            if (ringing) {
                Box(
                    Modifier.fillMaxWidth().background(Tvs.Red, TvsShape.Card)
                        .clickable(onClick = onOpenAlarm)
                        .padding(TvsSpace.s18),
                ) {
                    Text("Alarm ringing — tap to open", color = Tvs.White, fontSize = TvsFont.Lead, fontWeight = FontWeight.Black)
                }
            }

            // 1. Live checklist
            Card {
                CapsLabel("This phone", Tvs.Navy)
                checks.forEach { item -> CheckRow(item, onFix = { onFix(item) }) }
            }

            // 2. Brand-specific steps
            Card {
                CapsLabel("${guide.brand} — extra steps", Tvs.Navy)
                Text(
                    "Android can't report these, so do each one by hand, then run a test below.",
                    color = Tvs.Muted, fontSize = TvsFont.Caption,
                )
                guide.steps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberBadge(i + 1)
                        Spacer(Modifier.width(TvsSpace.s12))
                        Text(step.text, color = Tvs.Navy, fontSize = TvsFont.Body, modifier = Modifier.weight(1f))
                        if (step.intents.isNotEmpty()) {
                            Spacer(Modifier.width(TvsSpace.s8))
                            OutlinedButton(
                                onClick = { onOpenStep(step) },
                                modifier = Modifier.heightIn(min = TvsSize.Touch),
                                shape = TvsShape.Control,
                            ) { Text("Open", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Text(
                    "Never use \"Force stop\" on this app — Android then cancels its alarms until you open it again.",
                    color = Tvs.Red, fontSize = TvsFont.Caption, fontWeight = FontWeight.Bold,
                )
            }

            // 3. Real test alarms through the full alarm path
            Card {
                CapsLabel("Test", Tvs.Navy)
                Button(
                    onClick = { onTest(MINUTE_MS, "Lock the phone now and wait — it must ring even on silent.") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = TvsSize.TouchXL),
                    shape = TvsShape.Card,
                    colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
                ) { Text("Test alarm in 1 minute", fontSize = TvsFont.Heading, fontWeight = FontWeight.Black) }
                Text("Long tests (overnight, after swiping the app away, or after a reboot):", color = Tvs.Muted, fontSize = TvsFont.Caption)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    listOf(2, 6).forEach { h ->
                        OutlinedButton(
                            onClick = {
                                onTest(h * 60 * MINUTE_MS, "Swipe the app away or restart the phone — it must still ring on time.")
                            },
                            modifier = Modifier.weight(1f).heightIn(min = TvsSize.Touch),
                            shape = TvsShape.Control,
                        ) { Text("Test in $h h", fontWeight = FontWeight.Bold) }
                    }
                }
                message?.let { Text(it, color = Tvs.Blue, fontSize = TvsFont.BodySmall, fontWeight = FontWeight.Bold) }
            }

            // 4. What happened to the tests
            if (tests.isNotEmpty()) {
                Card {
                    CapsLabel("Test log", Tvs.Navy)
                    tests.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Set for ${Labels.clock(t.scheduledAt)}", color = Tvs.Navy, fontSize = TvsFont.Body, fontWeight = FontWeight.Bold)
                                Text(testStatus(t), color = statusColor(t), fontSize = TvsFont.Caption)
                            }
                            if (t.status == CheckpointStatus.PENDING) {
                                OutlinedButton(
                                    onClick = { onCancelTest(t.id) },
                                    modifier = Modifier.heightIn(min = TvsSize.Touch),
                                    shape = TvsShape.Control,
                                ) { Text("Cancel") }
                            }
                        }
                    }
                }
            }

            Text("Transfusion VS $versionName", color = Tvs.Muted, fontSize = TvsFont.Footnote)
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Tvs.White, TvsShape.Card).padding(TvsSpace.s16),
        verticalArrangement = Arrangement.spacedBy(TvsSpace.s14),
    ) { content() }
}

@Composable
private fun CheckRow(item: CheckItem, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(TvsSize.Dot).background(if (item.ok) Tvs.Green else Tvs.Red, CircleShape))
        Spacer(Modifier.width(TvsSpace.s12))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Tvs.Navy, fontSize = TvsFont.BodyLarge, fontWeight = FontWeight.Bold)
            Text(item.detail, color = Tvs.Muted, fontSize = TvsFont.Caption)
        }
        if (!item.ok) {
            Spacer(Modifier.width(TvsSpace.s8))
            Button(
                onClick = onFix,
                modifier = Modifier.heightIn(min = TvsSize.Touch),
                shape = TvsShape.Control,
                colors = ButtonDefaults.buttonColors(containerColor = Tvs.Red, contentColor = Tvs.White),
            ) { Text("Fix", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun NumberBadge(n: Int) {
    Box(Modifier.size(TvsSize.Badge).background(Tvs.Navy, CircleShape), contentAlignment = Alignment.Center) {
        Text("$n", color = Tvs.White, fontWeight = FontWeight.Black, fontSize = TvsFont.Caption)
    }
}

private fun lateness(t: CheckpointWithBed): String {
    val fired = t.firedAt ?: return ""
    val lateMin = (fired - t.scheduledAt) / MINUTE_MS
    return if (lateMin < 1) " (on time)" else " (+$lateMin min late)"
}

private fun testStatus(t: CheckpointWithBed): String = when (t.status) {
    CheckpointStatus.PENDING -> "Waiting…"
    CheckpointStatus.RINGING -> "Ringing now" + lateness(t)
    CheckpointStatus.ACKED -> "Rang ${t.firedAt?.let { Labels.clock(it) }}${lateness(t)} · acknowledged ${t.ackAt?.let { Labels.clock(it) }}"
    CheckpointStatus.MISSED -> "Rang ${t.firedAt?.let { Labels.clock(it) }}${lateness(t)} · no answer → missed" +
        (t.ackAt?.let { " · acknowledged late ${Labels.clock(it)}" } ?: "")
    CheckpointStatus.CANCELLED -> "Cancelled"
}

private fun statusColor(t: CheckpointWithBed): Color = when (t.status) {
    CheckpointStatus.ACKED -> Tvs.Green
    CheckpointStatus.MISSED, CheckpointStatus.RINGING -> Tvs.Red
    else -> Tvs.Muted
}

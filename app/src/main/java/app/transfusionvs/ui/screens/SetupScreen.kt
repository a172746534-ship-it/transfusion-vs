package app.transfusionvs.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.BuildConfig
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.app
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.WardBed
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.setup.CheckItem
import app.transfusionvs.setup.OemGuide
import app.transfusionvs.setup.OemGuides
import app.transfusionvs.setup.OemStep
import app.transfusionvs.setup.SetupChecks
import app.transfusionvs.ui.PRIMARY_TAG
import app.transfusionvs.ui.components.Alert
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.PrimaryButton
import app.transfusionvs.ui.components.SecondaryButton
import app.transfusionvs.ui.components.WardButton
import app.transfusionvs.ui.components.WardHeader
import app.transfusionvs.ui.theme.TvsShape
import app.transfusionvs.ui.theme.TvsSize
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Setup check: the ward's beds, a live permission checklist, per-brand steps, and real test alarms. */
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
    val wardDao = app.db.wardDao()
    val bedsFlow = remember { wardDao.observeBeds().map { list -> list.map { it.label } } }
    val beds by bedsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
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
        beds = beds,
        onAddBeds = { labels -> app.scope.launch { wardDao.addBeds(labels.map { WardBed(it) }) } },
        onRemoveBed = { label -> app.scope.launch { wardDao.removeBed(label) } },
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
    beds: List<String> = emptyList(),
    onAddBeds: (List<String>) -> Unit = {},
    onRemoveBed: (String) -> Unit = {},
    onBack: () -> Unit,
    onFix: (CheckItem) -> Unit,
    onOpenStep: (OemStep) -> Unit,
    onTest: (delayMs: Long, hint: String) -> Unit,
    onCancelTest: (Long) -> Unit,
    onOpenAlarm: () -> Unit,
    versionName: String,
) {
    val c = Ward.c
    Column(Modifier.fillMaxSize().background(c.bg)) {
        WardHeader(
            caps = "Setup check",
            title = "Make sure alarms can't be missed",
            subtitle = "${checks.count { it.ok }} of ${checks.size} ready",
            onBack = onBack,
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(TvsSpace.s12),
            verticalArrangement = Arrangement.spacedBy(TvsSpace.s12),
        ) {
            if (ringing) Alert("Alarm ringing — tap to open", c.dangerFill, c.onDangerFill, onOpenAlarm)

            // 1. Live checklist
            Panel {
                CapsLabel("This phone")
                checks.forEach { item -> CheckRow(item, onFix = { onFix(item) }) }
            }

            WardBedsPanel(beds, onAddBeds, onRemoveBed)

            // 2. Brand-specific steps
            Panel {
                CapsLabel("${guide.brand} — extra steps")
                Text("Android can't report these, so do each one by hand, then run a test below.", color = c.muted, style = WardType.Body)
                guide.steps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberBadge(i + 1)
                        Spacer(Modifier.width(TvsSpace.s12))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
                            Text(step.text, color = c.ink, style = WardType.Body)
                            if (step.intents.isNotEmpty()) {
                                SecondaryButton("Open phone settings", { onOpenStep(step) })
                            }
                        }
                    }
                }
                Text("Never use \"Force stop\" on this app — Android then cancels its alarms until you open it again.", color = c.ink, style = WardType.BodyBold)
            }

            // 3. Long tests through the full alarm path
            Panel {
                CapsLabel("Long tests")
                Text("Overnight, after swiping the app away, or after a reboot:", color = c.muted, style = WardType.Body)
                Row(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                    listOf(2, 6).forEach { h ->
                        SecondaryButton(
                            "Test in $h h",
                            { onTest(h * 60 * MINUTE_MS, "Swipe the app away or restart the phone — it must still ring on time.") },
                            Modifier.weight(1f),
                        )
                    }
                }
                message?.let { Text(it, color = c.ink, style = WardType.BodyBold) }
            }

            // 4. What happened to the tests
            if (tests.isNotEmpty()) {
                Panel {
                    CapsLabel("Test log")
                    tests.forEach { t ->
                        Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
                            Text("Set for ${Labels.clock(t.scheduledAt)}", color = c.ink, style = WardType.BodyBold)
                            Text(testStatus(t), color = statusColor(t), style = WardType.Body)
                            if (t.status == CheckpointStatus.PENDING) {
                                SecondaryButton("Cancel test at ${Labels.clock(t.scheduledAt)}", { onCancelTest(t.id) })
                            }
                        }
                    }
                }
            }

            Text("A S · Transfusion VS $versionName", color = c.muted, style = WardType.Body)
        }

        Box(Modifier.fillMaxWidth().testTag(PRIMARY_TAG).windowInsetsPadding(WindowInsets.navigationBars).padding(TvsSpace.s12)) {
            PrimaryButton("Test alarm in 1 minute", { onTest(MINUTE_MS, "Lock the phone now and wait — it must ring even on silent.") })
        }
    }
}

@Composable
private fun CheckRow(item: CheckItem, onFix: () -> Unit) {
    val c = Ward.c
    Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s6)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ✓ or ! in the badge, so the state never rests on colour alone.
            Box(Modifier.size(TvsSize.Badge).background(if (item.ok) c.ink else c.danger, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (item.ok) "✓" else "!", color = if (item.ok) c.card else c.onDanger, style = WardType.Lead)
            }
            Spacer(Modifier.width(TvsSpace.s12))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = c.ink, style = WardType.BodyBold)
                Text(item.detail, color = c.muted, style = WardType.Body)
            }
        }
        if (!item.ok) {
            WardButton(
                "Fix: ${item.title}", onFix, Modifier.fillMaxWidth(),
                fill = c.danger, content = c.onDanger, minHeight = TvsSize.Touch, style = WardType.ButtonSmall, shape = TvsShape.Control,
            )
        }
    }
}

@Composable
private fun NumberBadge(n: Int) {
    val c = Ward.c
    Box(Modifier.size(TvsSize.Badge).background(c.primary, CircleShape), contentAlignment = Alignment.Center) {
        Text("$n", color = c.onPrimary, style = WardType.Lead)
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

@Composable
private fun statusColor(t: CheckpointWithBed): Color = when (t.status) {
    CheckpointStatus.ACKED -> Ward.c.ink
    CheckpointStatus.MISSED, CheckpointStatus.RINGING -> Ward.c.danger
    else -> Ward.c.muted
}

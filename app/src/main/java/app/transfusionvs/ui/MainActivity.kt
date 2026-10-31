package app.transfusionvs.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.transfusionvs.alarm.AlarmActivity
import app.transfusionvs.app
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.ui.components.UndoBar
import app.transfusionvs.ui.components.rememberNight
import app.transfusionvs.ui.components.rememberNow
import app.transfusionvs.ui.screens.BedChoice
import app.transfusionvs.ui.screens.DetailScreen
import app.transfusionvs.ui.screens.HomeScreen
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.screens.SettingsScreen
import app.transfusionvs.ui.screens.SetupScreen
import app.transfusionvs.ui.screens.StartScreen
import app.transfusionvs.ui.screens.TurnDetailContent
import app.transfusionvs.ui.screens.TurnStartScreen
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.TvsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    /** A home-screen shortcut asked for this start flow. */
    private val shortcut = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        if (savedInstanceState == null) shortcut.value = intent?.action
        lifecycleScope.launch {
            // The settings file and database are opened off the main thread; the dark window shows meanwhile.
            val firstRun = withContext(Dispatchers.IO) {
                val s = app.settings
                val first = !s.setupSeen
                s.setupSeen = true
                app.db.openHelper.writableDatabase
                first
            }
            setContent {
                val night = rememberNight(app)
                LaunchedEffect(night) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                        navigationBarStyle = if (night) {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        },
                    )
                }
                TvsTheme(night = night) { AppNavigation(firstRun, shortcut.value) { shortcut.value = null } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcut.value = intent.action
    }

    override fun onResume() {
        super.onResume()
        // Opening the app always re-arms every alarm from the database (cheap and idempotent).
        app.scope.launch { app.engine.rearm(canStartService = true) }
    }

    companion object {
        const val ACTION_START_TRANSFUSION = "app.transfusionvs.action.START_TRANSFUSION"
        const val ACTION_START_TURNING = "app.transfusionvs.action.START_TURNING"
    }
}

/** The screens, as a simple back stack (no navigation library needed). */
private sealed interface Route {
    data object Home : Route
    data object Setup : Route
    data object Start : Route
    data object Settings : Route
    data object TurnStart : Route
    data class TurnDetail(val id: Long) : Route
    data class TurnEdit(val id: Long) : Route
    data class Detail(val id: Long) : Route
    data class Edit(val id: Long) : Route
    /** [id] = the bed to stop, or null to choose among running beds. */
    data class Reaction(val id: Long?) : Route
}

private fun encode(r: Route): String = when (r) {
    Route.Home -> "home"
    Route.Setup -> "setup"
    Route.Start -> "start"
    Route.Settings -> "settings"
    Route.TurnStart -> "turnstart"
    is Route.TurnDetail -> "turndetail:${r.id}"
    is Route.TurnEdit -> "turnedit:${r.id}"
    is Route.Detail -> "detail:${r.id}"
    is Route.Edit -> "edit:${r.id}"
    is Route.Reaction -> "reaction:${r.id ?: ""}"
}

private fun decode(s: String): Route {
    val arg = s.substringAfter(':', "").toLongOrNull()
    return when (s.substringBefore(':')) {
        "setup" -> Route.Setup
        "start" -> Route.Start
        "settings" -> Route.Settings
        "turnstart" -> Route.TurnStart
        "turndetail" -> arg?.let { Route.TurnDetail(it) } ?: Route.Home
        "turnedit" -> arg?.let { Route.TurnEdit(it) } ?: Route.Home
        "detail" -> arg?.let { Route.Detail(it) } ?: Route.Home
        "edit" -> arg?.let { Route.Edit(it) } ?: Route.Home
        "reaction" -> Route.Reaction(arg)
        else -> Route.Home
    }
}

@Composable
private fun AppNavigation(firstRun: Boolean, shortcut: String?, onShortcutUsed: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val vm: MainViewModel = viewModel { MainViewModel(app) }
    var stack by rememberSaveable(
        stateSaver = listSaver<List<Route>, String>(save = { it.map(::encode) }, restore = { it.map(::decode) }),
    ) { mutableStateOf(if (firstRun) listOf(Route.Home, Route.Setup) else listOf(Route.Home)) }

    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun home() { stack = listOf(Route.Home) }

    LaunchedEffect(shortcut) {
        when (shortcut) {
            MainActivity.ACTION_START_TRANSFUSION -> stack = listOf(Route.Home, Route.Start)
            MainActivity.ACTION_START_TURNING -> stack = listOf(Route.Home, Route.TurnStart)
        }
        if (shortcut != null) onShortcutUsed()
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val home by vm.home.collectAsStateWithLifecycle()
    val undo by vm.undo.collectAsStateWithLifecycle()
    val running = home.orEmpty().map { it.transfusion }.filter { it.status == TransfusionStatus.RUNNING }

    Box(Modifier.fillMaxSize()) {
        when (val r = stack.last()) {
            Route.Home -> HomeScreen(
                vm = vm,
                onStartTransfusion = { push(Route.Start) },
                onStartTurning = { push(Route.TurnStart) },
                onOpenTurnPlan = { push(Route.TurnDetail(it)) },
                onOpen = { push(Route.Detail(it)) },
                onReaction = { push(Route.Reaction(null)) },
                onSetup = { push(Route.Setup) },
                onSettings = { push(Route.Settings) },
            )
            Route.Setup -> SetupScreen(onBack = ::pop)
            Route.Settings -> SettingsScreen(onBack = ::pop, onSetup = { push(Route.Setup) })
            Route.Start -> StartScreen(vm, editing = null, onDone = ::home, onBack = ::pop)
            Route.TurnStart -> TurnStartScreen(
                vm, editing = null,
                onSubmit = { draft ->
                    vm.startTurnPlan(draft)
                    home()
                },
                onBack = ::pop,
            )
            is Route.TurnDetail -> {
                val flow = remember(r.id) { vm.turnPlan(r.id) }
                val item by flow.collectAsStateWithLifecycle(initialValue = null)
                val now by rememberNow(15_000)
                TurnDetailContent(
                    item = item, now = now, windowMin = vm.turnWindowMin,
                    onBack = ::pop,
                    onEdit = { push(Route.TurnEdit(r.id)) },
                    onStop = vm::stopTurnPlan,
                    onRestart = vm::restartTurnPlan,
                    onOpenRound = {
                        context.startActivity(Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                )
            }
            is Route.TurnEdit -> {
                val flow = remember(r.id) { vm.turnPlan(r.id) }
                val item by flow.collectAsStateWithLifecycle(initialValue = null)
                item?.plan?.let { plan ->
                    TurnStartScreen(
                        vm, editing = plan,
                        onSubmit = { draft ->
                            vm.editTurnPlan(r.id, draft)
                            pop()
                        },
                        onBack = ::pop,
                    )
                }
            }
            is Route.Detail -> DetailScreen(
                vm = vm,
                id = r.id,
                onBack = ::pop,
                onEdit = { push(Route.Edit(r.id)) },
                onReaction = { push(Route.Reaction(r.id)) },
            )
            is Route.Edit -> {
                // Only a running transfusion can be edited; otherwise go back.
                val t = running.firstOrNull { it.id == r.id }
                if (t != null) {
                    StartScreen(vm, editing = t, onDone = ::pop, onBack = ::pop)
                } else if (home != null) {
                    LaunchedEffect(r) { pop() }
                }
            }
            is Route.Reaction -> ReactionScreen(
                beds = running.filter { r.id == null || it.id == r.id }
                    .map { BedChoice(it.id, it.bedLabel, it.colorIndex) },
                onStopped = { id ->
                    vm.stopForReaction(id)
                    stack = listOf(Route.Home, Route.Detail(id))
                },
                onBack = ::pop,
            )
        }
        UndoBar(
            action = undo.firstOrNull(),
            onUndo = vm::undo,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(TvsSpace.s12),
        )
    }
}

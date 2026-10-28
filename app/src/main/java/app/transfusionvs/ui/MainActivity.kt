package app.transfusionvs.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.transfusionvs.app
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.ui.screens.BedChoice
import app.transfusionvs.ui.screens.DetailScreen
import app.transfusionvs.ui.screens.HomeScreen
import app.transfusionvs.ui.screens.LogScreen
import app.transfusionvs.ui.screens.ReactionScreen
import app.transfusionvs.ui.screens.SettingsScreen
import app.transfusionvs.ui.screens.SetupScreen
import app.transfusionvs.ui.screens.StartScreen
import app.transfusionvs.ui.theme.TvsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        // Setup check opens by itself on first launch (it stays reachable from Home).
        val firstRun = !app.settings.setupSeen
        app.settings.setupSeen = true
        setContent { TvsTheme { AppNavigation(firstRun) } }
    }

    override fun onResume() {
        super.onResume()
        // Opening the app always re-arms every alarm from the database (cheap and idempotent).
        app.scope.launch { app.engine.rearm(canStartService = true) }
    }
}

/** The screens, as a simple back stack (no navigation library needed). */
private sealed interface Route {
    data object Home : Route
    data object Setup : Route
    data object Start : Route
    data object Log : Route
    data object Settings : Route
    data class Detail(val id: Long) : Route
    data class Edit(val id: Long) : Route
    /** [id] = the bed to stop, or null to choose among running beds. */
    data class Reaction(val id: Long?) : Route
}

private fun encode(r: Route): String = when (r) {
    Route.Home -> "home"
    Route.Setup -> "setup"
    Route.Start -> "start"
    Route.Log -> "log"
    Route.Settings -> "settings"
    is Route.Detail -> "detail:${r.id}"
    is Route.Edit -> "edit:${r.id}"
    is Route.Reaction -> "reaction:${r.id ?: ""}"
}

private fun decode(s: String): Route {
    val arg = s.substringAfter(':', "").toLongOrNull()
    return when (s.substringBefore(':')) {
        "setup" -> Route.Setup
        "start" -> Route.Start
        "log" -> Route.Log
        "settings" -> Route.Settings
        "detail" -> arg?.let { Route.Detail(it) } ?: Route.Home
        "edit" -> arg?.let { Route.Edit(it) } ?: Route.Home
        "reaction" -> Route.Reaction(arg)
        else -> Route.Home
    }
}

@Composable
private fun AppNavigation(firstRun: Boolean) {
    val app = LocalContext.current.app
    val vm: MainViewModel = viewModel { MainViewModel(app) }
    var stack by rememberSaveable(
        stateSaver = listSaver<List<Route>, String>(save = { it.map(::encode) }, restore = { it.map(::decode) }),
    ) { mutableStateOf(if (firstRun) listOf(Route.Home, Route.Setup) else listOf(Route.Home)) }

    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun home() { stack = listOf(Route.Home) }

    BackHandler(enabled = stack.size > 1) { pop() }

    val home by vm.home.collectAsStateWithLifecycle()
    val running = home.orEmpty().map { it.transfusion }.filter { it.status == TransfusionStatus.RUNNING }

    when (val r = stack.last()) {
        Route.Home -> HomeScreen(
            vm = vm,
            onStart = { push(Route.Start) },
            onOpen = { push(Route.Detail(it)) },
            onReaction = { push(Route.Reaction(null)) },
            onSetup = { push(Route.Setup) },
            onLog = { push(Route.Log) },
            onSettings = { push(Route.Settings) },
        )
        Route.Setup -> SetupScreen(onBack = ::pop)
        Route.Log -> LogScreen(vm, onBack = ::pop)
        Route.Settings -> SettingsScreen(onBack = ::pop, onSetup = { push(Route.Setup) })
        Route.Start -> StartScreen(vm, editing = null, onDone = ::home, onBack = ::pop)
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
}

package spike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import java.awt.GraphicsEnvironment
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlinx.coroutines.delay

object State {
    var alarmVisible by mutableStateOf(false)
    var generation by mutableIntStateOf(0)
    var mainVisible by mutableStateOf(false)
    var clicks by mutableIntStateOf(0)
    @Volatile var alarmHwnd: HWND? = null
    @Volatile var prevProc: Pointer? = null
}

fun main(args: Array<String>) {
    val a = args.toList()
    when {
        "--installer-stop" in a -> exitProcess(Marker.installerStop(a))
        "--setup-check" in a -> exitProcess(itMode("setup-check|" + a.getOrElse(a.indexOf("--setup-check") + 1) { "" }))
        "--test-alarm" in a -> exitProcess(itMode("test-alarm"))
    }
    runApp(a)
}

/** IT modes: never take the mutex, never touch the data folder. */
private fun itMode(cmd: String): Int {
    val setupFile = if (cmd.startsWith("setup-check|")) File(cmd.substringAfter('|')) else null
    val h = U32.I.FindWindowW(WString(Hidden.CLASS), null)
    if (h == null) {
        setupFile?.writeText("not running\n")
        return 2
    }
    val data = Memory((cmd.length + 1) * 2L).also { it.setWideString(0, cmd) }
    val cds = COPYDATASTRUCT().apply { cbData = cmd.length * 2; lpData = data }
    cds.write()
    val res = LongByReference()
    val ok = U32.I.SendMessageTimeoutW(h, W.WM_COPYDATA, WPARAM(0), LPARAM(Pointer.nativeValue(cds.pointer)), W.SMTO_ABORTIFHUNG, 10_000, res)
    if (setupFile == null) return if (ok.toLong() != 0L) 0 else 2
    val deadline = System.currentTimeMillis() + 10_000
    while (!setupFile.exists() && System.currentTimeMillis() < deadline) Thread.sleep(100)
    if (!setupFile.exists()) { setupFile.writeText("not running\n"); return 2 }
    return 0
}

private val dataDir = File(System.getenv("LOCALAPPDATA") ?: ".", "TransfusionVS\\data")
private val heartbeat = File(dataDir, "heartbeat")

private fun runApp(a: List<String>) {
    Log.i("START args=$a pid=${ProcessHandle.current().pid()} admin=${Ident.admin} sid=${Ident.sid} user=${System.getProperty("user.name")} java=${System.getProperty("java.version")} renderApi=${System.getProperty("skiko.renderApi")}")
    Log.i("START jna.boot.library.path=${System.getProperty("jna.boot.library.path")} skiko.library.path=${System.getProperty("skiko.library.path")} tzdb=${java.time.zone.ZoneRulesProvider.getVersions("Africa/Cairo").lastKey()}")
    K32.I.SetErrorMode(0x0001 or 0x0002)
    Log.i("START WerSetFlags=0x${Integer.toHexString(K32.I.WerSetFlags(0x20))}")
    dataDir.mkdirs()
    Log.i("START SetCurrentDirectoryW=${K32.I.SetCurrentDirectoryW(WString(dataDir.path))} cwd=${File(".").canonicalPath}")
    dataDir.listFiles { f -> f.name.startsWith("hs_err_") }?.forEach { Log.i("START deleting crash file ${it.name}: ${it.delete()}") }
    Log.i("START AUMID=${Shell.I.SetCurrentProcessExplicitAppUserModelID(WString("TransfusionVS"))}")
    if (Marker.valid()) { Log.i("START installer marker valid: exit"); exitProcess(0) }
    val mutex = K32.I.CreateMutexW(null, false, WString("Local\\TransfusionVS"))
    if (K32.I.GetLastError() == W.ERROR_ALREADY_EXISTS) {
        secondLaunch(a, mutex!!)
    }
    startMain()
}

private fun secondLaunch(a: List<String>, mutex: HANDLE) {
    val h = U32.I.FindWindowW(WString(Hidden.CLASS), null)
    val res = LongByReference()
    val t0 = System.nanoTime()
    val answered = h != null && U32.I.SendMessageTimeoutW(h, Hidden.WM_PING, WPARAM(ProcessHandle.current().pid()), LPARAM(0), W.SMTO_ABORTIFHUNG, 5_000, res).toLong() != 0L && res.value == 1L
    Log.i("SECOND ping answered=$answered in ${(System.nanoTime() - t0) / 1_000_000} ms source=${a.firstOrNull() ?: "user"}")
    if (answered) {
        if (a.none { it.startsWith("--") }) {
            val pid = IntByReference().also { U32.I.GetWindowThreadProcessId(h!!, it) }.value
            Log.i("SECOND AllowSetForegroundWindow($pid)=${U32.I.AllowSetForegroundWindow(pid)}")
            U32.I.SendMessageTimeoutW(h!!, Hidden.WM_SHOW, WPARAM(0), LPARAM(0), W.SMTO_ABORTIFHUNG, 5_000, res)
        }
        exitProcess(0)
    }
    val hb = runCatching { heartbeat.readLines().associate { it.substringBefore('=') to it.substringAfter('=') } }.getOrDefault(emptyMap())
    val age = Uptime.unbiasedMs() - (hb["uptime"]?.toLongOrNull() ?: 0L)
    val pid = hb["pid"]?.toLongOrNull()
    Log.i("SECOND no answer; heartbeat age=$age ms pid=$pid")
    if (age <= 30_000) exitProcess(0)
    pid?.let { p -> ProcessHandle.of(p).ifPresent { Log.i("SECOND ending hung $p: ${it.destroyForcibly()}") } }
    Log.i("SECOND took over: wait mutex=${K32.I.WaitForSingleObject(mutex, 10_000)}")
}

private fun startMain() {
    Hidden.onShow = { SwingUtilities.invokeLater { State.mainVisible = true } }
    Hidden.onExit = { Log.i("EXIT on WM_ENDSESSION"); exitProcess(0) }
    Hidden.onCopyData = { text ->
        when {
            text.startsWith("setup-check|") -> File(text.substringAfter('|')).writeText("Keeps the PC awake while timers run\tActive\tok\nRestart task\tPresent\tok\n")
            text == "test-alarm" -> Scheduler.exec.schedule({ SwingUtilities.invokeLater { State.alarmVisible = true; State.generation++ } ; Log.i("TEST alarm shown") }, 60, TimeUnit.SECONDS)
        }
    }
    Hidden.start()
    Power.create()
    Power.set(system = true, display = false)
    Tasks.register(restart = true, at = Instant.now())
    Tasks.register(restart = false, at = Instant.now().plusSeconds(3600))
    Modules.write("modules-start.txt")
    Scheduler.exec.scheduleWithFixedDelay({
        SwingUtilities.invokeLater { runCatching { heartbeat.writeText("uptime=${Uptime.unbiasedMs()}\npid=${ProcessHandle.current().pid()}\n") } }
    }, 0, 5, TimeUnit.SECONDS)
    Scheduler.exec.scheduleWithFixedDelay({ if (Marker.valid()) { Log.i("MARKER valid: restore and exit"); exitProcess(0) } }, 1, 1, TimeUnit.SECONDS)
    thread(name = "commands", isDaemon = true) { commands() }
    if (File(spikeDir, "alarm-at-start").exists()) State.alarmVisible = true
    application(exitProcessOnExit = true) {
        Window(onCloseRequest = { State.mainVisible = false }, visible = State.mainVisible, title = "A S") {
            LaunchedEffect(State.mainVisible) { if (State.mainVisible) { window.toFront(); Log.i("MAIN shown, renderApi=${(window as ComposeWindow).renderApi}") } }
            Text("A S (spike)")
        }
        if (State.alarmVisible) key(State.generation) { AlarmWindow() }
    }
}

private fun commands() {
    while (true) {
        Thread.sleep(300)
        val files = spikeDir.listFiles { f -> f.name.startsWith("cmd-") && f.name.endsWith(".txt") }?.sortedBy { it.name } ?: continue
        for (f in files) {
            val c = runCatching { f.readText().trim() }.getOrDefault("")
            f.delete()
            Log.i("CMD '$c'")
            val parts = c.split(' ')
            when (parts[0]) {
                "alarm-show" -> SwingUtilities.invokeLater { State.alarmVisible = true }
                "alarm-hide" -> SwingUtilities.invokeLater { State.alarmVisible = false }
                "alarm-recreate" -> SwingUtilities.invokeLater { State.generation++ }
                "main-show" -> SwingUtilities.invokeLater { State.mainVisible = true }
                "block-ui" -> SwingUtilities.invokeLater { Log.i("UI blocked ${parts[1]} s"); Thread.sleep(parts[1].toLong() * 1000); Log.i("UI unblocked") }
                "audio" -> Audio.spike()
                "modules" -> Modules.write()
                "timer-on" -> { Hidden.requestReason(true); Power.set(true, false) }
                "timer-off" -> { Hidden.requestReason(false); Power.set(false, false) }
                "display-on" -> Power.set(true, true)
                "status" -> status()
                "crash" -> crash()
                "quit" -> { Log.i("QUIT"); exitProcess(0) }
            }
        }
    }
}

private fun status() {
    val h = State.alarmHwnd
    val sb = StringBuilder()
    sb.append("alarmHwnd=${h?.let { Pointer.nativeValue(it.pointer) } ?: 0}\n")
    if (h != null) {
        val r = RECT().also { U32.I.GetWindowRect(h, it) }
        val cloak = IntByReference().also { Dwm.I.DwmGetWindowAttribute(h, W.DWMWA_CLOAKED, it, 4) }.value
        sb.append("exstyle=0x${java.lang.Long.toHexString(Pointer.nativeValue(U32.I.GetWindowLongPtrW(h, W.GWL_EXSTYLE)))}\n")
        sb.append("style=0x${java.lang.Long.toHexString(Pointer.nativeValue(U32.I.GetWindowLongPtrW(h, W.GWL_STYLE)))}\n")
        sb.append("rect=${r.left},${r.top},${r.right},${r.bottom}\niconic=${U32.I.IsIconic(h)}\nvisible=${U32.I.IsWindowVisible(h)}\ncloaked=$cloak\n")
    }
    sb.append("foreground=${U32.I.GetForegroundWindow()?.let { Pointer.nativeValue(it.pointer) } ?: 0}\nclicks=${State.clicks}\ndoubleClickTime=${U32.I.GetDoubleClickTime()}\n")
    File(spikeDir, "status.txt").writeText(sb.toString())
}

@Suppress("DEPRECATION")
private fun crash() {
    val f = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").also { it.isAccessible = true }
    (f.get(null) as sun.misc.Unsafe).putAddress(0, 0)
}

private val alarmProc = WndProc { h, msg, wp, lp ->
    val sc = (wp.toLong() and 0xFFF0).toInt()
    when {
        msg == W.WM_CLOSE -> { Log.i("ALARM WM_CLOSE ignored"); LRESULT(0) }
        msg == W.WM_SYSCOMMAND && (sc == W.SC_CLOSE || sc == W.SC_MINIMIZE) -> { Log.i("ALARM WM_SYSCOMMAND 0x${Integer.toHexString(sc)} ignored"); LRESULT(0) }
        msg == W.WM_MOUSEACTIVATE -> LRESULT(W.MA_NOACTIVATE.toLong())
        else -> U32.I.CallWindowProcW(State.prevProc!!, h, msg, wp, lp)
    }
}

private fun decorate(window: ComposeWindow) {
    val h = HWND(Native.getWindowPointer(window))
    State.alarmHwnd = h
    val ex = Pointer.nativeValue(U32.I.GetWindowLongPtrW(h, W.GWL_EXSTYLE))
    val newEx = (ex or W.WS_EX_NOACTIVATE.toLong() or W.WS_EX_TOPMOST.toLong() or W.WS_EX_APPWINDOW.toLong()) and W.WS_EX_TOOLWINDOW.toLong().inv()
    U32.I.SetWindowLongPtrW(h, W.GWL_EXSTYLE, Pointer(newEx))
    val st = Pointer.nativeValue(U32.I.GetWindowLongPtrW(h, W.GWL_STYLE))
    U32.I.SetWindowLongPtrW(h, W.GWL_STYLE, Pointer(st and W.WS_SYSMENU.toLong().inv()))
    State.prevProc = U32.I.SetWindowLongPtrW(h, W.GWLP_WNDPROC, alarmProc)
    U32.I.SetWindowPos(h, W.HWND_TOPMOST, 0, 0, 0, 0, W.SWP_NOMOVE or W.SWP_NOSIZE or W.SWP_NOACTIVATE or 0x20)
    U32.I.FlashWindowEx(FLASHWINFO().apply { hwnd = h; dwFlags = W.FLASHW_ALL or W.FLASHW_TIMERNOFG })
    File(spikeDir, "alarm-hwnd.txt").writeText(Pointer.nativeValue(h.pointer).toString())
    Log.i("ALARM decorated hwnd=${Pointer.nativeValue(h.pointer)} ex 0x${java.lang.Long.toHexString(ex)} -> 0x${java.lang.Long.toHexString(newEx)} renderApi=${window.renderApi} foreground=${U32.I.GetForegroundWindow()?.pointer}")
}

@Composable
private fun ApplicationScope.AlarmWindow() {
    val wa = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val state = rememberWindowState(position = WindowPosition((wa.x + wa.width - 412).dp, wa.y.dp), size = DpSize(412.dp, wa.height.dp))
    Window(
        onCloseRequest = { Log.i("ALARM onCloseRequest ignored") },
        state = state, title = "A S — alarm", undecorated = true, alwaysOnTop = true, focusable = false, resizable = false,
    ) {
        LaunchedEffect(Unit) {
            decorate(window)
            while (true) {
                delay(500)
                val h = State.alarmHwnd ?: continue
                val cloak = IntByReference().also { Dwm.I.DwmGetWindowAttribute(h, W.DWMWA_CLOAKED, it, 4) }.value
                if (cloak != 0 || U32.I.IsIconic(h) || !U32.I.IsWindowVisible(h)) {
                    Log.i("ALARM tick: cloaked=$cloak iconic=${U32.I.IsIconic(h)} visible=${U32.I.IsWindowVisible(h)} -> recreate")
                    State.generation++
                    break
                }
                U32.I.SetWindowPos(h, W.HWND_TOPMOST, 0, 0, 0, 0, W.SWP_NOMOVE or W.SWP_NOSIZE or W.SWP_NOACTIVATE)
            }
        }
        Box(
            Modifier.fillMaxSize().background(Color(0xFF13233F)).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Press) Log.i("ALARM press clickCount=${(e.nativeEvent as? java.awt.event.MouseEvent)?.clickCount} at=${e.changes.first().position}")
                    }
                }
            },
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Repositioning round", color = Color.White, fontSize = 28.sp)
                Text("clicks: ${State.clicks}", color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                Button(onClick = { State.clicks++; Log.i("CLICK done #${State.clicks}") }, modifier = Modifier.fillMaxWidth().height(96.dp)) { Text("Done ✓", fontSize = 28.sp) }
            }
        }
    }
}

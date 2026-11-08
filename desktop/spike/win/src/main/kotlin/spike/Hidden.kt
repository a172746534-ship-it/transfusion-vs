package spike

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser.MSG
import com.sun.jna.platform.win32.Wtsapi32
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * The hidden top-level window (WS_POPUP, WS_EX_TOOLWINDOW, never shown; not message-only) on its own thread with its
 * own message loop. Broadcasts reach top-level windows only (trap 10).
 */
object Hidden {
    const val CLASS = "TransfusionVS.Hidden"
    const val WM_PING = W.WM_APP + 1
    const val WM_SHOW = W.WM_APP + 2
    const val WM_REASON = W.WM_APP + 3

    @Volatile var hwnd: HWND? = null
    @Volatile var timerRunning = true
    @Volatile private var lastRefusal = 0L
    private val ready = CountDownLatch(1)
    var onShow: () -> Unit = {}
    var onCopyData: (String) -> Unit = {}
    var onExit: () -> Unit = {}

    private val proc = WndProc { h, msg, wp, lp -> handle(h, msg, wp, lp) }

    fun start() {
        thread(name = "hidden-window", isDaemon = true) {
            val inst = K32.I.GetModuleHandleW(null)
            val wc = WNDCLASSEXW().apply { lpfnWndProc = proc; hInstance = inst; lpszClassName = WString(CLASS) }
            val atom = U32.I.RegisterClassExW(wc)
            val h = U32.I.CreateWindowExW(W.WS_EX_TOOLWINDOW, WString(CLASS), WString("TransfusionVS"), W.WS_POPUP, 0, 0, 0, 0, null, null, inst, null)
            hwnd = h
            Log.i("HIDDEN created atom=$atom hwnd=${h?.pointer} thread=${K32.I.GetCurrentThreadId()}")
            if (h != null) {
                Log.i("HIDDEN WTSRegisterSessionNotification=${Wtsapi32.INSTANCE.WTSRegisterSessionNotification(h, Wtsapi32.NOTIFY_FOR_THIS_SESSION)}")
                Log.i("HIDDEN RegisterSuspendResumeNotification=${U32.I.RegisterSuspendResumeNotification(h, 0)}")
                Log.i("HIDDEN RegisterPowerSettingNotification(display)=${U32.I.RegisterPowerSettingNotification(h, W.GUID_CONSOLE_DISPLAY_STATE, 0)}")
                setReason(timerRunning)
            }
            ready.countDown()
            val msg = MSG()
            while (U32.I.GetMessageW(msg, null, 0, 0) > 0) {
                U32.I.TranslateMessage(msg)
                U32.I.DispatchMessageW(msg)
            }
            Log.i("HIDDEN loop ended")
        }
        ready.await(5, TimeUnit.SECONDS)
    }

    /** ShutdownBlockReasonCreate/Destroy must run on the window's own thread. */
    private fun setReason(on: Boolean) {
        val h = hwnd ?: return
        val ok = if (on) U32.I.ShutdownBlockReasonCreate(h, WString("Timers are still running")) else U32.I.ShutdownBlockReasonDestroy(h)
        Log.i("HIDDEN shutdown reason ${if (on) "created" else "destroyed"}: $ok")
    }

    fun requestReason(on: Boolean) {
        timerRunning = on
        hwnd?.let { U32.I.PostMessageW(it, WM_REASON, WPARAM(if (on) 1 else 0), LPARAM(0)) }
    }

    private fun handle(h: HWND, msg: Int, wp: WPARAM, lp: LPARAM): LRESULT {
        when (msg) {
            W.WM_TIMECHANGE -> Log.i("MSG WM_TIMECHANGE zone(before re-detect)=${java.util.TimeZone.getDefault().id}").also { redetectZone() }
            W.WM_SETTINGCHANGE -> Log.i("MSG WM_SETTINGCHANGE wParam=${wp.toLong()} lParam=${lp.toLong().let { if (it != 0L) runCatching { Pointer(it).getWideString(0) }.getOrNull() else null }}").also { redetectZone() }
            W.WM_DISPLAYCHANGE -> Log.i("MSG WM_DISPLAYCHANGE ${lp.toLong() and 0xFFFF}x${(lp.toLong() shr 16) and 0xFFFF}")
            W.WM_POWERBROADCAST -> Log.i("MSG WM_POWERBROADCAST event=0x${wp.toLong().toString(16)}")
            W.WM_WTSSESSION_CHANGE -> Log.i("MSG WM_WTSSESSION_CHANGE code=${wp.toLong()}")
            W.WM_QUERYENDSESSION -> {
                val closeApp = (lp.toLong() and W.ENDSESSION_CLOSEAPP.toLong()) != 0L
                val now = System.currentTimeMillis()
                val refuse = timerRunning && !closeApp && now - lastRefusal > 30_000
                if (refuse) lastRefusal = now
                val buf = CharArray(256)
                val size = com.sun.jna.ptr.IntByReference(buf.size)
                val q = U32.I.ShutdownBlockReasonQuery(h, buf, size)
                Log.i("MSG WM_QUERYENDSESSION lParam=0x${lp.toLong().toString(16)} closeApp=$closeApp answer=${if (refuse) "FALSE" else "TRUE"} reasonQuery=$q '${String(buf, 0, (size.value - 1).coerceIn(0, buf.size))}'")
                return W.lresult(if (refuse) 0 else 1)
            }
            W.WM_ENDSESSION -> {
                Log.i("MSG WM_ENDSESSION ending=${wp.toLong()} lParam=0x${lp.toLong().toString(16)}")
                if (wp.toLong() != 0L) onExit()
                return W.lresult(0)
            }
            WM_REASON -> { setReason(wp.toLong() != 0L); return W.lresult(0) }
            WM_PING -> {
                // Answer only after a round trip through the scheduler thread and the UI thread.
                val done = CountDownLatch(1)
                Scheduler.exec.execute { SwingUtilities.invokeLater { done.countDown() } }
                val ok = done.await(4, TimeUnit.SECONDS)
                Log.i("MSG PING from wParam=${wp.toLong()} roundTrip=$ok")
                return W.lresult(if (ok) 1 else 0)
            }
            WM_SHOW -> { Log.i("MSG SHOW (user start)"); onShow(); return W.lresult(1) }
            W.WM_COPYDATA -> {
                val text = runCatching {
                    val cds = COPYDATASTRUCT(Pointer(lp.toLong()))
                    String(cds.lpData!!.getCharArray(0, cds.cbData / 2))
                }.getOrElse { "?" }
                Log.i("MSG WM_COPYDATA '$text'")
                onCopyData(text)
                return W.lresult(1)
            }
        }
        return U32.I.DefWindowProcW(h, msg, wp, lp)
    }

    fun redetectZone() {
        System.clearProperty("user.timezone")
        java.util.TimeZone.setDefault(null)
        Log.i("ZONE re-detected: ${java.util.TimeZone.getDefault().id} offset=${java.time.ZoneId.systemDefault().rules.getOffset(java.time.Instant.now())}")
    }
}

object Scheduler {
    val exec: java.util.concurrent.ScheduledExecutorService = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "scheduler").also { it.isDaemon = true } }
}

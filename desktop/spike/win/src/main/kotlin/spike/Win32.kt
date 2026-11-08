package spike

import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.platform.win32.WinUser.MSG
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary

// Minimal Win32 bindings for the spike, with exact W names (no function mapper surprises).

fun interface WndProc : StdCallLibrary.StdCallCallback {
    fun callback(hwnd: HWND, msg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT
}

@Structure.FieldOrder("cbSize", "style", "lpfnWndProc", "cbClsExtra", "cbWndExtra", "hInstance", "hIcon", "hCursor", "hbrBackground", "lpszMenuName", "lpszClassName", "hIconSm")
class WNDCLASSEXW : Structure() {
    @JvmField var cbSize = 0
    @JvmField var style = 0
    @JvmField var lpfnWndProc: Callback? = null
    @JvmField var cbClsExtra = 0
    @JvmField var cbWndExtra = 0
    @JvmField var hInstance: Pointer? = null
    @JvmField var hIcon: Pointer? = null
    @JvmField var hCursor: Pointer? = null
    @JvmField var hbrBackground: Pointer? = null
    @JvmField var lpszMenuName: WString? = null
    @JvmField var lpszClassName: WString? = null
    @JvmField var hIconSm: Pointer? = null
    init { cbSize = size() }
}

@Structure.FieldOrder("dwData", "cbData", "lpData")
class COPYDATASTRUCT : Structure {
    @JvmField var dwData: Pointer? = null
    @JvmField var cbData = 0
    @JvmField var lpData: Pointer? = null
    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}

@Structure.FieldOrder("cbSize", "hwnd", "dwFlags", "uCount", "dwTimeout")
class FLASHWINFO : Structure() {
    @JvmField var cbSize = 0
    @JvmField var hwnd: HWND? = null
    @JvmField var dwFlags = 0
    @JvmField var uCount = 0
    @JvmField var dwTimeout = 0
    init { cbSize = size() }
}

/** REASON_CONTEXT with POWER_REQUEST_CONTEXT_SIMPLE_STRING; padded to the union's full size. */
@Structure.FieldOrder("Version", "Flags", "SimpleReasonString", "pad1", "pad2")
class REASON_CONTEXT(reason: String) : Structure() {
    @JvmField var Version = 0
    @JvmField var Flags = 1
    @JvmField var SimpleReasonString: WString? = WString(reason)
    @JvmField var pad1: Pointer? = null
    @JvmField var pad2: Pointer? = null
}

@Structure.FieldOrder("left", "top", "right", "bottom")
class RECT : Structure() {
    @JvmField var left = 0
    @JvmField var top = 0
    @JvmField var right = 0
    @JvmField var bottom = 0
}

interface U32 : StdCallLibrary {
    fun RegisterClassExW(wc: WNDCLASSEXW): Short
    fun CreateWindowExW(ex: Int, cls: WString, title: WString, style: Int, x: Int, y: Int, w: Int, h: Int, parent: HWND?, menu: Pointer?, inst: Pointer?, param: Pointer?): HWND?
    fun DefWindowProcW(hwnd: HWND, msg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT
    fun GetMessageW(msg: MSG, hwnd: HWND?, min: Int, max: Int): Int
    fun TranslateMessage(msg: MSG): Boolean
    fun DispatchMessageW(msg: MSG): LRESULT
    fun PostMessageW(hwnd: HWND, msg: Int, wParam: WPARAM, lParam: LPARAM): Boolean
    fun SendMessageTimeoutW(hwnd: HWND, msg: Int, wParam: WPARAM, lParam: LPARAM, flags: Int, timeout: Int, result: LongByReference?): LRESULT
    fun FindWindowW(cls: WString?, title: WString?): HWND?
    fun GetWindowThreadProcessId(hwnd: HWND, pid: IntByReference): Int
    fun ShutdownBlockReasonCreate(hwnd: HWND, reason: WString): Boolean
    fun ShutdownBlockReasonDestroy(hwnd: HWND): Boolean
    fun ShutdownBlockReasonQuery(hwnd: HWND, buf: CharArray?, size: IntByReference): Boolean
    fun RegisterSuspendResumeNotification(recipient: HANDLE, flags: Int): Pointer?
    fun RegisterPowerSettingNotification(recipient: HANDLE, guid: Guid.GUID, flags: Int): Pointer?
    fun GetWindowLongPtrW(hwnd: HWND, index: Int): Pointer?
    fun SetWindowLongPtrW(hwnd: HWND, index: Int, value: Pointer?): Pointer?
    fun SetWindowLongPtrW(hwnd: HWND, index: Int, value: WndProc): Pointer?
    fun CallWindowProcW(prev: Pointer, hwnd: HWND, msg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT
    fun SetWindowPos(hwnd: HWND, after: HWND?, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean
    fun ShowWindow(hwnd: HWND, cmd: Int): Boolean
    fun IsIconic(hwnd: HWND): Boolean
    fun IsWindowVisible(hwnd: HWND): Boolean
    fun GetForegroundWindow(): HWND?
    fun AllowSetForegroundWindow(pid: Int): Boolean
    fun SetForegroundWindow(hwnd: HWND): Boolean
    fun FlashWindowEx(info: FLASHWINFO): Boolean
    fun GetDoubleClickTime(): Int
    fun GetWindowRect(hwnd: HWND, rect: RECT): Boolean

    companion object { val I: U32 = Native.load("user32", U32::class.java) }
}

interface K32 : StdCallLibrary {
    fun PowerCreateRequest(ctx: REASON_CONTEXT): HANDLE?
    fun PowerSetRequest(h: HANDLE, type: Int): Boolean
    fun PowerClearRequest(h: HANDLE, type: Int): Boolean
    fun QueryUnbiasedInterruptTime(t: LongByReference): Boolean
    fun SetErrorMode(mode: Int): Int
    fun WerSetFlags(flags: Int): Int
    fun SetCurrentDirectoryW(path: WString): Boolean
    fun CreateMutexW(attrs: Pointer?, owner: Boolean, name: WString): HANDLE?
    fun GetLastError(): Int
    fun GetModuleHandleW(name: WString?): Pointer?
    fun GetCurrentThreadId(): Int
    fun WaitForSingleObject(h: HANDLE, ms: Int): Int

    companion object { val I: K32 = Native.load("kernel32", K32::class.java) }
}

interface Dwm : StdCallLibrary {
    fun DwmGetWindowAttribute(hwnd: HWND, attr: Int, value: IntByReference, size: Int): Int
    companion object { val I: Dwm = Native.load("dwmapi", Dwm::class.java) }
}

interface Shell : StdCallLibrary {
    fun IsUserAnAdmin(): Boolean
    fun SetCurrentProcessExplicitAppUserModelID(id: WString): Int
    companion object { val I: Shell = Native.load("shell32", Shell::class.java) }
}

object W {
    const val WS_POPUP = 0x80000000.toInt()
    const val WS_SYSMENU = 0x00080000
    const val WS_EX_TOOLWINDOW = 0x00000080
    const val WS_EX_TOPMOST = 0x00000008
    const val WS_EX_NOACTIVATE = 0x08000000
    const val WS_EX_APPWINDOW = 0x00040000
    const val GWL_STYLE = -16
    const val GWL_EXSTYLE = -20
    const val GWLP_WNDPROC = -4
    const val WM_CLOSE = 0x0010
    const val WM_QUERYENDSESSION = 0x0011
    const val WM_ENDSESSION = 0x0016
    const val WM_SETTINGCHANGE = 0x001A
    const val WM_TIMECHANGE = 0x001E
    const val WM_MOUSEACTIVATE = 0x0021
    const val WM_COPYDATA = 0x004A
    const val WM_POWERBROADCAST = 0x0218
    const val WM_WTSSESSION_CHANGE = 0x02B1
    const val WM_DISPLAYCHANGE = 0x007E
    const val WM_SYSCOMMAND = 0x0112
    const val WM_APP = 0x8000
    const val SC_CLOSE = 0xF060
    const val SC_MINIMIZE = 0xF020
    const val MA_NOACTIVATE = 3
    const val ENDSESSION_CLOSEAPP = 0x1
    const val SMTO_ABORTIFHUNG = 0x2
    const val SWP_NOSIZE = 0x1
    const val SWP_NOMOVE = 0x2
    const val SWP_NOACTIVATE = 0x10
    const val SWP_SHOWWINDOW = 0x40
    const val SW_SHOWNOACTIVATE = 4
    const val FLASHW_ALL = 3
    const val FLASHW_TIMERNOFG = 12
    const val DWMWA_CLOAKED = 14
    const val ERROR_ALREADY_EXISTS = 183
    val HWND_TOPMOST = HWND(Pointer.createConstant(-1L))
    val GUID_CONSOLE_DISPLAY_STATE: Guid.GUID = Guid.GUID.fromString("{6FE69556-704A-47A0-8F24-C28D936FDA47}")

    fun lresult(v: Long) = LRESULT(v)
}

/** A COM interface pointer: call vtable slot [index] with `this` first. */
class Com(val p: Pointer) {
    fun call(index: Int, vararg args: Any?): Int {
        val vtbl = p.getPointer(0)
        val fn = Function.getFunction(vtbl.getPointer(index.toLong() * Native.POINTER_SIZE), Function.ALT_CONVENTION)
        return fn.invokeInt(arrayOf<Any?>(p, *args))
    }
    fun qi(iid: Guid.GUID): Com? {
        val out = PointerByReference()
        return if (call(0, iid, out) == 0) Com(out.value) else null
    }
    fun release() { call(2) }
}

fun guidMemory(g: Guid.GUID): Memory = Memory(16).also { m -> g.write(); m.write(0, g.pointer.getByteArray(0, 16), 0, 16) }

@Suppress("unused") private val keep = listOf(FloatByReference::class, IntByReference::class)

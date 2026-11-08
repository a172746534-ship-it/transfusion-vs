package spike

import com.sun.jna.Native
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Psapi
import com.sun.jna.platform.win32.WinDef.HMODULE
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.concurrent.thread

/** Power requests held by an app-lifetime object (trap 8). */
object Power {
    private var handle: HANDLE? = null
    private val reason = REASON_CONTEXT("Timers are still running")

    /** Created on a short-lived thread on purpose: the request must outlive the thread that made it. */
    fun create() {
        thread(name = "power-create") {
            handle = K32.I.PowerCreateRequest(reason)
            Log.i("POWER PowerCreateRequest=${handle?.pointer} lastError=${K32.I.GetLastError()}")
        }.join()
    }

    fun set(system: Boolean, display: Boolean) {
        val h = handle ?: return
        val s = if (system) K32.I.PowerSetRequest(h, 1) else K32.I.PowerClearRequest(h, 1)
        val d = if (display) K32.I.PowerSetRequest(h, 0) else K32.I.PowerClearRequest(h, 0)
        Log.i("POWER systemRequired=$system ($s) displayRequired=$display ($d)")
    }
}

object Ident {
    val sid: String by lazy {
        val tok = WinNT.HANDLEByReference()
        Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(), WinNT.TOKEN_QUERY, tok)
        Advapi32Util.getTokenAccount(tok.value).sidString
    }
    val admin: Boolean by lazy { runCatching { Shell.I.IsUserAnAdmin() }.getOrDefault(false) }
}

object Tasks {
    private fun exe() = ProcessHandle.current().info().command().orElse("TransfusionVS.exe")

    fun xml(restart: Boolean, at: Instant): String {
        val start = at.truncatedTo(ChronoUnit.SECONDS).toString() // UTC, ends with Z
        val sid = Ident.sid
        val triggers = if (restart) {
            "<LogonTrigger><Enabled>true</Enabled><UserId>$sid</UserId></LogonTrigger>" +
                "<TimeTrigger><Repetition><Interval>PT1M</Interval><StopAtDurationEnd>false</StopAtDurationEnd></Repetition><StartBoundary>$start</StartBoundary><Enabled>true</Enabled></TimeTrigger>"
        } else {
            "<TimeTrigger><Repetition><Interval>PT1M</Interval><StopAtDurationEnd>false</StopAtDurationEnd></Repetition><StartBoundary>$start</StartBoundary><Enabled>true</Enabled></TimeTrigger>"
        }
        return """<?xml version="1.0" encoding="UTF-16"?>
<Task version="1.2" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task">
  <RegistrationInfo><Description>A S</Description></RegistrationInfo>
  <Triggers>$triggers</Triggers>
  <Principals><Principal id="Author"><UserId>$sid</UserId><LogonType>InteractiveToken</LogonType><RunLevel>LeastPrivilege</RunLevel></Principal></Principals>
  <Settings>
    <MultipleInstancesPolicy>Parallel</MultipleInstancesPolicy>
    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
    <AllowHardTerminate>true</AllowHardTerminate>
    <StartWhenAvailable>true</StartWhenAvailable>
    <RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>
    <IdleSettings><StopOnIdleEnd>false</StopOnIdleEnd><RestartOnIdle>false</RestartOnIdle></IdleSettings>
    <AllowStartOnDemand>true</AllowStartOnDemand>
    <Enabled>true</Enabled>
    <Hidden>false</Hidden>
    <RunOnlyIfIdle>false</RunOnlyIfIdle>
    <WakeToRun>${!restart}</WakeToRun>
    <ExecutionTimeLimit>PT0S</ExecutionTimeLimit>
    <Priority>4</Priority>
  </Settings>
  <Actions Context="Author"><Exec><Command>"${exe()}"</Command><Arguments>${if (restart) "--watchdog" else "--backstop"}</Arguments></Exec></Actions>
</Task>
"""
    }

    fun register(restart: Boolean, at: Instant) {
        val name = (if (restart) "TransfusionVS Restart " else "TransfusionVS Backstop ") + Ident.sid
        val f = File(spikeDir, if (restart) "restart.xml" else "backstop.xml")
        f.writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + xml(restart, at).toByteArray(StandardCharsets.UTF_16LE))
        val r = run("schtasks", "/create", "/tn", name, "/xml", f.path, "/f")
        Log.i("TASK create '$name' at=$at exit=${r.first} out=${r.second.trim()}")
        val q = run("schtasks", "/query", "/tn", name, "/xml")
        File(spikeDir, "readback-${if (restart) "restart" else "backstop"}.xml").writeText(q.second)
        Log.i("TASK readback '$name' exit=${q.first}")
    }

    fun run(vararg cmd: String): Pair<Int, String> = runCatching {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
        p.waitFor() to out
    }.getOrElse { -1 to it.toString() }
}

object Modules {
    fun write(name: String = "modules.txt") {
        val proc = Kernel32.INSTANCE.GetCurrentProcess()
        val mods = arrayOfNulls<HMODULE>(1024)
        val needed = IntByReference()
        Psapi.INSTANCE.EnumProcessModules(proc, mods, mods.size * Native.POINTER_SIZE, needed)
        val n = needed.value / Native.POINTER_SIZE
        val paths = (0 until n).mapNotNull { i ->
            val buf = CharArray(1024)
            val len = Psapi.INSTANCE.GetModuleFileNameExW(proc, mods[i], buf, buf.size)
            if (len > 0) String(buf, 0, len) else null
        }
        File(spikeDir, name).writeText(paths.joinToString("\n"))
        Log.i("MODULES $n written; temp-like=${paths.filter { it.contains("\\Temp\\", true) || it.contains(".skiko", true) || it.contains("jna", true) && !it.contains("Program Files", true) }}")
    }
}

object Uptime {
    fun unbiasedMs(): Long = LongByReference().also { K32.I.QueryUnbiasedInterruptTime(it) }.value / 10_000
}

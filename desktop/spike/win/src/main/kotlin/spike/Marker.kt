package spike

import java.io.File
import java.time.Instant

/** %ProgramData%\TransfusionVS\installing: written by --installer-stop (SYSTEM), read by every running app. */
object Marker {
    val file = File(System.getenv("ProgramData") ?: "C:\\ProgramData", "TransfusionVS\\installing")
    private val log = File(System.getenv("ProgramData") ?: "C:\\ProgramData", "TransfusionVS\\spike-installer.log")

    private fun ilog(s: String) = runCatching { log.parentFile.mkdirs(); log.appendText("${Instant.now()} $s\n") }

    /** Valid = younger than 15 minutes and its PID is a running msiexec.exe. */
    fun valid(): Boolean {
        if (!file.exists()) return false
        val props = runCatching { file.readLines().associate { it.substringBefore('=') to it.substringAfter('=') } }.getOrDefault(emptyMap())
        val at = props["time"]?.toLongOrNull() ?: return false
        val pid = props["pid"]?.toLongOrNull() ?: return false
        if (System.currentTimeMillis() - at > 15 * 60_000) return false
        val p = ProcessHandle.of(pid).orElse(null) ?: return false
        return p.isAlive && p.info().command().orElse("").endsWith("msiexec.exe", ignoreCase = true)
    }

    fun installerStop(args: List<String>): Int {
        val parent = ProcessHandle.current().parent().orElse(null)
        ilog("installer-stop args=$args parent=${parent?.pid()} ${parent?.info()?.command()?.orElse("?")} user=${System.getProperty("user.name")}")
        if ("--clear" in args) { ilog("clear: deleted=${file.delete()}"); return 0 }
        // The custom action runs under msiexec; walk up to the msiexec.exe that owns the install.
        var msi: ProcessHandle? = parent
        while (msi != null && !msi.info().command().orElse("").endsWith("msiexec.exe", true)) msi = msi.parent().orElse(null)
        file.parentFile.mkdirs()
        file.writeText("time=${System.currentTimeMillis()}\npid=${msi?.pid() ?: parent?.pid() ?: 0}\n")
        ilog("marker written pid=${msi?.pid()} exists=${file.exists()}")
        val me = ProcessHandle.current().pid()
        fun others() = ProcessHandle.allProcesses().filter { it.pid() != me && it.info().command().orElse("").endsWith("TransfusionVS.exe", true) }.toList()
        val deadline = System.currentTimeMillis() + 20_000
        while (others().isNotEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(250)
        val left = others()
        left.forEach { ilog("ending ${it.pid()} ${it.info().command().orElse("")}"); it.destroyForcibly() }
        ilog("others exited in time: ${left.isEmpty()}")
        if ("--remove-tasks" in args) {
            val list = Tasks.run("schtasks", "/query", "/fo", "csv", "/nh").second
            val names = list.lines().mapNotNull { l -> l.split("\",\"").firstOrNull()?.trim('"') }.filter { it.startsWith("\\TransfusionVS ") }.distinct()
            names.forEach { ilog("delete task '$it' -> ${Tasks.run("schtasks", "/delete", "/tn", it, "/f").first}") }
        }
        return 0
    }
}

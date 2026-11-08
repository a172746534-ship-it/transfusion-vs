package spike

import java.io.File
import java.time.Instant

/** Spike log only (the product writes no log file). */
object Log {
    var file: File = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"), "TransfusionVS/spike/log.txt")

    @Synchronized
    fun i(msg: String) {
        runCatching {
            file.parentFile.mkdirs()
            file.appendText("${Instant.now()} [${Thread.currentThread().name}] $msg\n")
        }
    }
}

val spikeDir: File get() = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"), "TransfusionVS/spike").also { it.mkdirs() }

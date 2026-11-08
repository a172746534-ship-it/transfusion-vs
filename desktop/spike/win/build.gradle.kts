// Spike items 2-10: a Windows prototype of the integrations, packaged as an app image named TransfusionVS
// (the WiX template in ../wix turns it into the MSI). Spike only; deleted before the final full run.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin { jvmToolchain(21) }

val appVersion = (findProperty("spikeVersion") as String?) ?: "1.0.0"
val reference = rootDir.resolve("../../app/src/main/res")

// JNA and the SQLite driver must load their DLLs from the install folder, never unpacked to %TEMP% (§3).
val nativeResources by tasks.registering(Copy::class) {
    val jna = configurations.runtimeClasspath.get().filter { it.name.startsWith("jna-5") }
    from(jna.map { zipTree(it) }) { include("com/sun/jna/win32-x86-64/jnidispatch.dll"); eachFile { path = name } }
    into(layout.buildDirectory.dir("app-resources/windows-x64"))
    includeEmptyDirs = false
}

val soundResources by tasks.registering(Copy::class) {
    from(reference.resolve("raw")) { include("*.wav") }
    into(layout.buildDirectory.dir("generated-res/raw"))
}
sourceSets.main { resources.srcDir(layout.buildDirectory.dir("generated-res")) }
tasks.named("processResources") { dependsOn(soundResources) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
}

compose.desktop {
    application {
        mainClass = "spike.MainKt"
        jvmArgs += listOf(
            "-Dskiko.renderApi=" + ((findProperty("renderApi") as String?) ?: "SOFTWARE_FAST"),
            "-XX:ErrorFile=hs_err_%p.log",
            "-XX:-CreateCoredumpOnCrash",
            "-Djna.nounpack=true",
            "-Djna.nosys=true",
            "-Djna.boot.library.path=\$APPDIR/resources",
            "-Dfile.encoding=UTF-8",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "TransfusionVS"
            packageVersion = appVersion
            modules("java.desktop", "jdk.localedata", "jdk.unsupported", "java.naming", "java.sql")
            appResourcesRootDir.set(layout.buildDirectory.dir("app-resources"))
        }
    }
}

tasks.matching { it.name.startsWith("prepareAppResources") || it.name == "createDistributable" }.configureEach { dependsOn(nativeResources) }

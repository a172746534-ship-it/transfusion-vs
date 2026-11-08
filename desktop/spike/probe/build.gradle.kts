// Spike item 11: Compose Desktop 1.12.2 APIs the plan relies on (vector XML, fonts from bytes, test scene size and density).
plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val reference = rootDir.resolve("../../app/src/main/res")

val copyResources by tasks.registering(Sync::class) {
    from(reference) { include("drawable/ic_*.xml", "font/*.ttf") }
    into(layout.buildDirectory.dir("probe-resources"))
}

kotlin { jvmToolchain(21) }

sourceSets.test { resources.srcDir(layout.buildDirectory.dir("probe-resources")) }
tasks.named("processTestResources") { dependsOn(copyResources) }

dependencies {
    testImplementation(compose.desktop.currentOs)
    testImplementation(compose.material3)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

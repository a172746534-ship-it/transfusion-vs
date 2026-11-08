// Spike item 11: Room 2.8.5 on a Kotlin JVM module with the bundled SQLite driver, the reference's own entities
// and DAOs copied unchanged. KSP arguments here; the Room Gradle plugin is tried separately in :roomplugin.
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

val reference = rootDir.resolve("../../app/src/main/java/app/transfusionvs")

val copyReference by tasks.registering(Sync::class) {
    from(reference) {
        include("schedule/**", "data/Entities.kt", "data/Turns.kt", "data/Custom.kt", "data/Ward.kt", "data/Dao.kt", "data/TurnDao.kt")
    }
    into(layout.buildDirectory.dir("reference/app/transfusionvs"))
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(layout.buildDirectory.dir("reference"))
        kotlin.srcDir(rootDir.resolve("stubs"))
    }
}

ksp {
    arg("room.schemaLocation", layout.buildDirectory.dir("schemas").get().asFile.path)
    arg("room.generateKotlin", "true")
}

tasks.matching { it.name.startsWith("ksp") || it.name == "compileKotlin" }.configureEach { dependsOn(copyReference) }

dependencies {
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.sqlite:sqlite-bundled:2.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

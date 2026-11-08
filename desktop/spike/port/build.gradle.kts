// Spike item 1: the reference's ui/ and alarm composables, copied UNCHANGED from app/ at build time, compiled
// against Compose Desktop. The compile is expected to fail; every error is recorded (it sizes class (b)).
plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val reference = rootDir.resolve("../../app/src/main/java/app/transfusionvs")

val copyReference by tasks.registering(Sync::class) {
    from(reference) {
        include(
            "schedule/**",
            "data/Entities.kt", "data/Turns.kt", "data/Custom.kt", "data/Ward.kt", "data/Dao.kt", "data/TurnDao.kt",
            "alarm/RingSpec.kt", "alarm/Undo.kt", "alarm/AlarmActivity.kt",
            "ui/**",
        )
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

tasks.named("compileKotlin") { dependsOn(copyReference) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation("org.jetbrains.compose.ui:ui-backhandler:1.12.2")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
}

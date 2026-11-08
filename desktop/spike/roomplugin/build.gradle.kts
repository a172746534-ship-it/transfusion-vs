// Spike item 11: does the Room Gradle plugin accept a plain Kotlin JVM module? (Compile and schema export only.)
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("androidx.room")
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
        kotlin.srcDir(rootDir.resolve("room/src/main/kotlin"))
    }
}

room {
    schemaDirectory(layout.buildDirectory.dir("schemas").get().asFile.path)
    generateKotlin = true
}

tasks.matching { it.name.startsWith("ksp") || it.name == "compileKotlin" }.configureEach { dependsOn(copyReference) }

dependencies {
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.sqlite:sqlite-bundled:2.6.2")
    ksp("androidx.room:room-compiler:2.8.5")
}

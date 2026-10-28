// Versions verified against Google Maven / Maven Central on 2026-10-28.
// AGP 9 has built-in Kotlin support: the kotlin-android plugin must NOT be applied.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Pins the Kotlin compiler used by AGP's built-in Kotlin (AGP 9.4 alone brings 2.2.10).
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    id("androidx.room") version "2.8.5" apply false
}

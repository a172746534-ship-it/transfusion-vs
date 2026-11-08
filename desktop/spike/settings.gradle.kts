// Step 0, the spike (docs/DESKTOP_APP.md §7 step 4). Deleted before the final full run.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tvs-spike"
include(":port", ":room", ":roomplugin", ":probe", ":win")

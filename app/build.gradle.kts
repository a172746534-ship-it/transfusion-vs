plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
    id("io.github.takahirom.roborazzi")
}

// CI sets these; local builds fall back to safe defaults.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val tagVersion = System.getenv("GITHUB_REF_NAME")
    ?.takeIf { System.getenv("GITHUB_REF_TYPE") == "tag" }
    ?.removePrefix("v")

android {
    namespace = "app.transfusionvs"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.transfusionvs"
        minSdk = 26
        targetSdk = 37
        versionCode = runNumber
        versionName = tagVersion ?: "dev-$runNumber"
    }

    // One fixed release key, decoded from GitHub Secrets by the workflow.
    // If the secrets are missing, the release build fails — it never ships unsigned.
    signingConfigs {
        create("release") {
            System.getenv("KEYSTORE_FILE")?.let { path ->
                storeFile = file(path)
                storeType = "pkcs12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // reliability over size (see SPEC decisions log)
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric screenshot tests render the real resources (fonts, icons).
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    // Exported schemas let later versions migrate the database without losing data.
    schemaDirectory("$projectDir/schemas")
}

roborazzi {
    // Reference screenshots live in the repo so CI can verify "no visual change".
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    testImplementation("junit:junit:4.13.2")

    // Screenshot tests: Robolectric renders the composables on the JVM, Roborazzi saves/compares PNGs.
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.77.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.77.0")
}

import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

configure<ApplicationExtension> {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }

    // The languages the app is translated into are read from its resource folders by the Android
    // Gradle Plugin, which generates the LocaleConfig from them, so that the system settings screen
    // can offer exactly those languages for this app. The default one is declared in
    // app/src/main/res/resources.properties.
    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk {
            version = release(libs.versions.minSdk.get().toInt())
        }
        targetSdk {
            version = release(libs.versions.targetSdk.get().toInt())
        }
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The app's own release key, used whenever it is available: CI passes it through the
    // environment (see .github/workflows/release.yml), and a local build takes it from the
    // environment or from gradle.properties. Without it the release build falls back to the debug
    // key so that it can still be installed locally.
    val releaseKeystorePath = providers.environmentVariable("RELEASE_KEYSTORE_FILE").orNull
        ?: providers.gradleProperty("releaseKeystoreFile").orNull
    val releaseKeystorePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
        ?: providers.gradleProperty("releaseKeystorePassword").orNull
    val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
        ?: providers.gradleProperty("releaseKeyAlias").orNull
    val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
        ?: providers.gradleProperty("releaseKeyPassword").orNull
    val hasReleaseSigning = listOf(
        releaseKeystorePath,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            // Debuggable on purpose, and it is the reason this build cold-starts slowly: a debuggable
            // app is run interpreted and JIT-compiled (ART does not use the AOT-compiled code), which
            // costs ~2s on the test device against ~0.8s for the same code built without it. Measure
            // cold start on a release build (or after flipping this flag), never on this one.
            isDebuggable = true
            isMinifyEnabled = false

            buildConfigField(type = "boolean", name = "HAS_LOGS_ENABLED", value = "true")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            // Signed with the app's release key when one is configured, and with the debug key
            // otherwise so that a build without it can still be installed locally.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            buildConfigField(type = "boolean", name = "HAS_LOGS_ENABLED", value = "false")
        }
    }

    compileOptions {
        sourceCompatibility(libs.versions.jvmTarget.get())
        targetCompatibility(libs.versions.jvmTarget.get())
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        // Required for Robolectric
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
    lint {
        disable += listOf(
            "MissingTranslation",
            "ExtraTranslation",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {
    implementation(project(":core:annotation"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:impl"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:impl"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.google.hilt.android)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    ksp(libs.google.hilt.compiler)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization)
    implementation(platform(libs.square.okhttp.bom))
    implementation(libs.square.okhttp)
    implementation(libs.timber)

    // For now, we are restricted to running Compose tests for debug builds only
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Pull in test fixtures from other modules
    testImplementation(testFixtures(project(":core:common")))
    testImplementation(testFixtures(project(":core:data")))
    testImplementation(testFixtures(project(":core:network")))
    testImplementation(testFixtures(project(":core:ui")))

    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.google.hilt.android.testing)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.robolectric.robolectric)
    testImplementation(libs.square.turbine)
}

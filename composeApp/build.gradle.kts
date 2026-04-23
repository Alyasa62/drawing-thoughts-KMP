import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 *  GLOBAL KOTLIN CONFIG
 */
configure<KotlinMultiplatformExtension> {
    jvmToolchain(17) // Stable for KMP + Compose
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {

        // Shared Code
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Icons
            implementation(compose.materialIconsExtended)

            // Room
            implementation(libs.androidx.room.runtime)

            // File handling
            implementation(libs.filekit.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        //  Android Specific
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }

        // Desktop Specific
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

/**
 *  ANDROID CONFIG
 */
android {
    namespace = "com.yasadevs.drawingthoughts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yasadevs.drawingthoughts"
        minSdk = 24
        targetSdk = 35

        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     *  Clean APK/AAB Naming
     */
    base {
        val appName = "DrawingThoughts"
        val versionCode = defaultConfig.versionCode
        val versionName = defaultConfig.versionName
        archivesName = "$appName-v$versionCode($versionName)"
    }

    /**
     *  For Language and Localization
     */
    bundle {
        language.enableSplit = false
        density.enableSplit = true
        abi.enableSplit = true
    }

    /**
     *  Build Types
     */
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            isDebuggable = false
        }

        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
    }

    /**
     * ⚙️ Java Compatibility
     */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    /**
     *  Packaging Cleanup
     */
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    /**
     * 🔧 Build Features
     */
    buildFeatures {
        buildConfig = true
    }
}

/**
 * ⚡ KSP + ROOM CONFIG
 */
dependencies {
    debugImplementation(compose.uiTooling)

    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

/**
 *  Room Schema Export
 */
room {
    schemaDirectory("$projectDir/schemas")
}

/**
 *  Desktop Packaging
 */
compose.desktop {
    application {
        mainClass = "com.yasadevs.drawingthoughts.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.yasadevs.drawingthoughts"
            packageVersion = "1.0.2"
        }
    }
}
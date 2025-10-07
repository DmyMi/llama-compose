import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import cloud.dmytrominochkin.gradle.version.AppVersion
import cloud.dmytrominochkin.gradle.BuildPlatform
import cloud.dmytrominochkin.gradle.currentBuildPlatform
import cloud.dmytrominochkin.gradle.version.tasks.XcodeVersionTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.wire)
    alias(libs.plugins.conveyor)
    id("cloud.dmytrominochkin.plugins.project-version")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.okio)
            implementation(libs.datastore.core)
            implementation(libs.androidx.navigation)
            implementation(libs.markdown.core)
            implementation(libs.markdown.m3)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.koin)
            implementation(libs.bundles.ktor)
            implementation(libs.kottie)
            // FIXME Exclude Netty as we're not using it anyway but it is part of Koog for whatever reason
            // FIXME Exclude MCP java client due to android build bug in 0.4.2
            implementation(libs.koog)
            implementation(projects.llama)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.bundles.jvm.logging)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

// FIXME Exclude Netty as we're not using it anyway but it is part of Koog for whatever reason
// FIXME Exclude MCP java client due to android build bug in 0.4.2
configurations.all {
    exclude(group = "io.netty", module = "*")
    exclude(group = "io.modelcontextprotocol", module = "kotlin-sdk-client-jvm")
}

android {
    namespace = "cloud.dmytrominochkin.ai.llamacompose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cloud.dmytrominochkin.ai.llamacompose"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = (project.version as AppVersion).getNumericAppVersion()
        versionName = project.version.toString()

        setProperty("archivesBaseName", "$applicationId-v$versionName($versionCode)")
    }

    val keystoreFile: String? = System.getenv("ANDROID_KEYSTORE_FILE")
        ?: findProperty("ANDROID_KEYSTORE_FILE") as? String
    signingConfigs {
        getByName("debug") {
        }

        // TODO: check when Idea linter is not going crazy for using lambda instead of Action impl.
        if (!keystoreFile.isNullOrBlank()) {
            create("release", Action {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                    ?: findProperty("ANDROID_KEYSTORE_PASSWORD") as? String
                keyAlias = System.getenv("ANDROID_KEYSTORE_ALIAS")
                    ?: findProperty("ANDROID_KEYSTORE_ALIAS") as? String
                keyPassword = System.getenv("ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD")
                    ?: findProperty("ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD") as? String
                enableV2Signing = true
            })
        } else {
            println(">>> ANDROID_KEYSTORE_FILE environment variable / property not found. Release build will be unsigned. <<<")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        buildTypes {
            getByName("debug") {
                signingConfig = signingConfigs.getByName("debug")
                applicationIdSuffix = ".debug"
                versionNameSuffix = "-debug"
                resValue("string", "app_name", "Llama Compose Debug")
            }
            release {
                isMinifyEnabled = false
                isDebuggable = false
                if (signingConfigs.findByName("release") != null) {
                    signingConfig = signingConfigs.getByName("release")
                }
                resValue("string", "app_name", "Llama Compose")
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        // We're only building ARM64 v8
        abi {
            enableSplit = false
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "cloud.dmytrominochkin.ai.llamacompose.resources"
    generateResClass = always
}


compose.desktop {
    application {
        mainClass = "cloud.dmytrominochkin.ai.llamacompose.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LlamaCompose"
            packageVersion = project.version.toString()

            appResourcesRootDir.set(project.layout.projectDirectory.dir("desktopResources"))

            macOS {
                iconFile.set(project.layout.projectDirectory.dir("icons").file("AppIcon.icns"))
            }
            windows {
                iconFile.set(project.layout.projectDirectory.dir("icons").file("AppIcon.ico"))
            }
            linux {
                iconFile.set(project.layout.projectDirectory.dir("icons").file("AppIcon.png"))
            }

            // TODO: Fix proguard minifier
            buildTypes.release.proguard {
                isEnabled.set(false)
            }

            // TODO: Figure out what modules are really used
//            modules(
//                "java.naming",
//                "java.management",
//                "java.instrument",
//                "java.net.http",
//                "jdk.unsupported",
//            )
            includeAllModules = true
        }
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}

// Task to copy native libraries to desktop resources
tasks.register<Copy>("copyNativeLibrariesToDesktop") {
    group = "llama"
    description = "Copy native libraries to desktop resources directory"

    // Determine build type (debug/release) in a configuration-cache friendly way
    val buildTypeProp = (findProperty("buildType") as? String)?.lowercase()
    val isReleaseBuild = buildTypeProp == "release"
    val buildTypeDirName = if (isReleaseBuild) "releaseShared" else "debugShared"
    val buildTypeCap = if (isReleaseBuild) "Release" else "Debug"

    val arch = System.getProperty("os.arch").lowercase()

    val isArm64 = arch.contains("aarch64") || arch.contains("arm64")

    val platformDir = when (currentBuildPlatform) {
        BuildPlatform.Windows -> "windows"
        BuildPlatform.Linux -> "linux"
        BuildPlatform.MacOS -> "macos"
        else -> throw IllegalStateException("Unsupported operating system")
    }

    val kNativeTargetDir = when (currentBuildPlatform) {
        BuildPlatform.Windows -> "mingwX64"
        BuildPlatform.Linux -> "linuxX64"
        BuildPlatform.MacOS -> if (isArm64) "macosArm64" else "macosX64"
        else -> throw IllegalStateException("Unsupported target for Kotlin/Native")
    }
    val kNativeTargetTaskSuffix = when (kNativeTargetDir) {
        "mingwX64" -> "MingwX64"
        "linuxX64" -> "LinuxX64"
        "macosArm64" -> "MacosArm64"
        "macosX64" -> "MacosX64"
        else -> error("Unexpected target dir: $kNativeTargetDir")
    }

    val (nativeLibPattern, fatLlamaFileName) = when (currentBuildPlatform) {
        BuildPlatform.Windows -> Pair("*.dll", "fatllama.dll")
        BuildPlatform.Linux -> Pair("*.so", "libfatllama.so")
        else -> Pair("*.dylib", "libfatllama.dylib")
    }

    // Ensure native builds are available before copying
    dependsOn(":llama:link${buildTypeCap}Shared$kNativeTargetTaskSuffix")

    // Copy all llama.cpp shared libs for the current platform
    from("../llama/native/llama.cpp/build-desktop/bin") {
        include(nativeLibPattern)
    }

    // Copy fatllama shared lib from Kotlin/Native build
    from("../llama/build/bin/$kNativeTargetDir/$buildTypeDirName") {
        include(fatLlamaFileName)
    }

    // On Windows, also copy MinGW runtime DLLs required by fatllama
    if (currentBuildPlatform == BuildPlatform.Windows) {
        val mingw64Bin = System.getenv("MINGW64_BIN")?.let { File(it) }
        if (mingw64Bin == null || !mingw64Bin.isDirectory) {
            error("""
                Setup 'MINGW64_BIN' environment variable. E.g., in Msys2 mingw64 terminal run
                export MINGW64_BIN=$(cygpath -u "C:\msys64\mingw64\bin")
            """.trimIndent())
        }

        val mingwRuntimeDlls = listOf(
            "libstdc++-6.dll",
            "libgcc_s_seh-1.dll",
            "libwinpthread-1.dll",
            "libgomp-1.dll",
        )

        from(mingw64Bin) {
            mingwRuntimeDlls.forEach { include(it) }
        }
    }

    into("desktopResources/$platformDir")

    val finalFolder = file("desktopResources/$platformDir")
    doLast {
        val typeLabel = if (isReleaseBuild) "release" else "debug"
        println("Copied native libraries to desktop resources for $platformDir ($typeLabel):")
        finalFolder.listFiles()?.forEach { f ->
            if (f.extension in listOf("dll", "so", "dylib")) {
                println("  - ${f.name}")
            }
        }
    }
}

tasks.register<XcodeVersionTask>("updateXcodeVersionConfig") {
    group = "xcode"
    xcodeProject.set("iosApp")
}

/*
   Copyright 2025 Dmytro Minochkin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
import cloud.dmytrominochkin.gradle.AndroidGpu
import cloud.dmytrominochkin.gradle.BuildPlatform
import cloud.dmytrominochkin.gradle.currentBuildPlatform
import org.jetbrains.compose.internal.utils.getLocalProperty
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    id("cloud.dmytrominochkin.plugins.llamacpp")
}

val buildMacOsSharedLibs =
    (System.getenv("LLAMA_BUILD_MACOS_SHARED") ?: findProperty("llama.build.macos-shared") as? String)?.toBoolean() ?: false

val iosDevRoot   = layout.projectDirectory.asFile.resolve("native/llama.cpp/build-ios-device")
val iosSimRoot   = layout.projectDirectory.asFile.resolve("native/llama.cpp/build-ios-sim")
val androidRoot  = layout.projectDirectory.asFile.resolve("native/llama.cpp/build-android")
val desktopRoot      = layout.projectDirectory.asFile.resolve("native/llama.cpp/build-desktop")

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    androidNativeArm64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    mingwX64()

    jvm("desktop")

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("commonJvm") {
                withJvm()
                withAndroidTarget()
            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        // Configure C interop for llama.cpp
        val family = konanTarget.family
        val targetName = konanTarget.name
        compilations.getByName("main") {

            val llama by cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/llama.def"))
                includeDirs(
                    project.file("native/llama.cpp/include"),
                    project.file("native/llama.cpp/ggml/include"),
                )

                when (family) {
                    // For iOS we're building a static framework, as it is easier to integrate
                    Family.IOS -> {
                        val isSim = (targetName.contains("sim", ignoreCase = true) ||
                            targetName.contains("x64", ignoreCase = true))
                        val root = if (isSim) iosSimRoot else iosDevRoot
                        val kind = if (isSim) "Release-iphonesimulator" else "Release-iphoneos"
                        listOf(
                            root.resolve("src/$kind"),
                            root.resolve("ggml/src/$kind"),
                            root.resolve("ggml/src/ggml-metal/$kind"),
                            root.resolve("ggml/src/ggml-blas/$kind"),
                        ).forEach { dir ->
                            extraOpts("-libraryPath", dir.absolutePath)
                        }

                        listOf(
                            "libllama.a",
                            "libggml.a",
                            "libggml-base.a",
                            "libggml-cpu.a",
                            "libggml-metal.a",
                            "libggml-blas.a",
                        ).forEach { archive ->
                            extraOpts("-staticLibrary", archive)
                        }

                        // Core linker options are set in .def file + binaries
                    }
                    // For Macos we're building a fat library, but we can do with full dynamic linking
                    Family.OSX -> {
                        if (buildMacOsSharedLibs) {
                            listOf(
                                desktopRoot.resolve("bin"),
                            ).forEach { dir ->
                                extraOpts("-libraryPath", dir.absolutePath)
                            }
                        } else {
                            listOf(
                                desktopRoot.resolve("src"),
                                desktopRoot.resolve("ggml/src"),
                                desktopRoot.resolve("ggml/src/ggml-blas"),
                                desktopRoot.resolve("ggml/src/ggml-metal"),
                            ).forEach { dir ->
                                extraOpts("-libraryPath", dir.absolutePath)
                            }

                            listOf(
                                "libllama.a",
                                "libggml.a",
                                "libggml-base.a",
                                "libggml-cpu.a",
                                "libggml-metal.a",
                                "libggml-blas.a",
                            ).forEach { archive ->
                                extraOpts("-staticLibrary", archive)
                            }
                        }

                        // Core linker options are set in .def file + binaries
                    }

                    Family.ANDROID -> {
                        // .def: libraryPaths.android_arm64
                        listOf(
                            androidRoot.resolve("bin")
                        ).forEach { dir ->
                            extraOpts("-libraryPath", dir.absolutePath)
                        }

                        val hostPlatform = when(currentBuildPlatform) {
                            BuildPlatform.MacOS -> "darwin"
                            BuildPlatform.Linux -> "linux"
                            BuildPlatform.Windows -> "windows"
                            else -> throw IllegalStateException("Unsupported operating system")
                        }
                        // Android headers
                        compilerOpts(
                            "-I${getLocalProperty("sdk.dir")}/ndk/${libs.versions.android.ndkVersion.get()}/toolchains/llvm/prebuilt/${hostPlatform}-x86_64/sysroot/usr/include",
                             "-D__ANDROID_API__=${libs.versions.android.minSdk.get()}"
                        )

                        // Core linker options are set in .def file + binaries
                    }

                    Family.MINGW -> {
                        listOf(
                            desktopRoot.resolve("src"),
                            desktopRoot.resolve("ggml/src"),
                            desktopRoot.resolve("ggml/src/ggml-vulkan"),
                        ).forEach { dir ->
                            extraOpts("-libraryPath", dir.absolutePath)
                        }

                        // Core linker options are set in .def file + binaries
                    }

                    Family.LINUX -> {
                        listOf(
                            desktopRoot.resolve("bin")
                        ).forEach { dir ->
                            extraOpts("-libraryPath", dir.absolutePath)
                        }

                        // Core linker options are set in .def file + binaries
                    }

                    else -> error("Platform not configured yet")
                }
            }
        }
        binaries {
            if (family != Family.IOS) {
                sharedLib {
                    baseName = "fatllama"
                    // This is for final linking of binaries
                    if (family == Family.ANDROID) {
                        linkerOpts(
                            "-L${androidRoot.resolve("bin").absolutePath}"
                        )
                    }
                    if (family == Family.OSX) {
                        if (buildMacOsSharedLibs) {
                            linkerOpts(
                                "-L${desktopRoot.resolve("bin").absolutePath}",
                                "-lllama",
                                "-lggml",
                                "-lggml-base",
                                "-lggml-cpu",
                                "-lggml-metal",
                                "-lggml-blas",
                                // Ensure dependent dylibs are resolved from the same folder as fatllama
                                "-Wl,-rpath,@loader_path"
                            )
                        }
                    }
                    if (family == Family.LINUX) {
                        linkerOpts(
                            "-L${desktopRoot.resolve("bin").absolutePath}",
                            // Ensure dependent .so libs are resolved from the same folder as fatllama
                            "-Wl,-rpath,\$ORIGIN"
                        )
                    }
                    if (family == Family.MINGW) {
                        linkerOpts(
                            "-L${desktopRoot.resolve("src").absolutePath}",
                            "-L${desktopRoot.resolve("ggml/src").absolutePath}",
                            "-L${desktopRoot.resolve("ggml/src").resolve("ggml-vulkan").absolutePath}"
                        )
                    }
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
                implementation(libs.kotlinx.serialization.json)
                api(libs.logging)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val commonJvmMain by getting {
            dependencies {
                // JVM-specific dependencies if needed
                compileOnly(libs.jna)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(dependencies.variantOf(libs.jna) {
                    artifactType("aar")
                })
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies if needed
                implementation(libs.jna)
            }
        }

        val nativeMain by getting {
            dependencies {
                // Native-specific dependencies if needed
            }
        }
    }
}

android {
    namespace = "cloud.dmytrominochkin.ai.llama"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = libs.versions.android.ndkVersion.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        ndk {
            // It runs on x64, but I don't have ChromeOs to test & fix issue
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            // Needs to be here for native tasks to run
        }
    }

    val jniLibDir =
        File(project.layout.buildDirectory.asFile.get(), arrayOf("generated", "jniLibs").joinToString(File.separator))
    sourceSets {
        getByName("debug") {
            jniLibs.srcDir(File(jniLibDir, "Debug"))
        }
        getByName("release") {
            jniLibs.srcDir(File(jniLibDir, "Release"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

llamaCpp {
    llamaCppDir.set(project.layout.projectDirectory.dir("native/llama.cpp"))

    ios {
        simulator {
            architectures.set("arm64;x86_64")
            sysroot.set("iphonesimulator")
            deploymentTarget.set("16.4")
        }
        device {
            architectures.set("arm64")
            sysroot.set("iphoneos")
            deploymentTarget.set("16.4")
        }
    }

    desktop {
        // override defaults if needed
        macOsSharedLibs.set(buildMacOsSharedLibs)
    }

    android {
        // Provide NDK location from Android plugin
        ndkDirectory.set(project.android.ndkDirectory)
        // Optional overrides
        androidPlatform.set("android-${libs.versions.android.minSdk.get()}")
        gpuType.set(AndroidGpu.Vulkan)
    }
}

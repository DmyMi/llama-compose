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
package cloud.dmytrominochkin.gradle.llamacpp

import cloud.dmytrominochkin.gradle.BuildPlatform
import cloud.dmytrominochkin.gradle.llamacpp.tasks.LlamaCppDesktopBuildTask
import cloud.dmytrominochkin.gradle.llamacpp.tasks.LlamaCppIosBuildTask
import cloud.dmytrominochkin.gradle.llamacpp.tasks.LlamaCppAndroidBuildTask
import cloud.dmytrominochkin.gradle.opencl.tasks.OpenClIcdLoaderBuildTask
import cloud.dmytrominochkin.gradle.AndroidGpu
import cloud.dmytrominochkin.gradle.currentBuildPlatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register
import org.gradle.api.tasks.Copy
import java.io.File


@Suppress("unused")
class LlamaCppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("llamaCpp", LlamaCppExtension::class.java, project.objects)

        ext.llamaCppDir.convention(project.layout.projectDirectory.dir("native/llama.cpp"))

        // iOS conventions
        val ios = ext.ios
        ios.simulator.enabled.convention(true)
        ios.simulator.sysroot.convention("iphonesimulator")
        ios.simulator.architectures.convention("arm64;x86_64")
        ios.simulator.buildDir.convention(ext.llamaCppDir.dir("build-ios-sim"))
        ios.simulator.deploymentTarget.convention("16.4")

        ios.device.enabled.convention(true)
        ios.device.sysroot.convention("iphoneos")
        ios.device.architectures.convention("arm64")
        ios.device.buildDir.convention(ext.llamaCppDir.dir("build-ios-device"))
        ios.device.deploymentTarget.convention("16.4")

        // Desktop conventions
        ext.desktop.enabled.convention(true)
        ext.desktop.buildDir.convention(ext.llamaCppDir.dir("build-desktop"))
        when (currentBuildPlatform) {
            BuildPlatform.MacOS -> {
                ext.desktop.enableMetal.convention(true)
                ext.desktop.enableCuda.convention(false)
                ext.desktop.enableOpenMP.convention(false)
                ext.desktop.enableBlas.convention(true)
                ext.desktop.enableVulkan.convention(false)
                ext.desktop.architectures.convention("arm64;x86_64")
                ext.desktop.macOsSharedLibs.convention(false)
            }
            BuildPlatform.Linux -> {
                ext.desktop.enableMetal.convention(false)
                ext.desktop.enableCuda.convention(false)
                ext.desktop.enableOpenMP.convention(false)
                ext.desktop.enableBlas.convention(true)
                ext.desktop.enableVulkan.convention(true)
                ext.desktop.architectures.convention("x86_64")
                ext.desktop.macOsSharedLibs.convention(false)
            }
            BuildPlatform.Windows -> {
                ext.desktop.enableMetal.convention(false)
                ext.desktop.enableCuda.convention(false)
                ext.desktop.enableOpenMP.convention(true)
                ext.desktop.enableBlas.convention(true)
                ext.desktop.enableVulkan.convention(true)
                ext.desktop.architectures.convention("x64")
                ext.desktop.macOsSharedLibs.convention(false)
            }
            else -> throw IllegalStateException("Unsupported operating system")
        }

        // Android conventions
        val android = ext.android
        android.enabled.convention(true)
        android.buildDir.convention(ext.llamaCppDir.dir("build-android"))
        android.androidPlatform.convention("android-28")
        android.abi.convention("arm64-v8a")
        android.hostArch.convention("x86_64")
        android.gpuType.convention(AndroidGpu.None)

        // iOS variant tasks
        val buildIosSimulator = project.tasks.register<LlamaCppIosBuildTask>("buildLlamaCppIosSimulator") {
            group = "llama"
            description = "Builds static library for llama.cpp (iOS simulator)."
            llamaCppDir.set(ext.llamaCppDir.map { it.asFile })
            buildDir.set(ios.simulator.buildDir)
            sysroot.set(ios.simulator.sysroot)
            architectures.set(ios.simulator.architectures)
            deploymentTarget.set(ios.simulator.deploymentTarget)
            staticLib.set(buildDir.flatMap { dir -> sysroot.map { s -> dir.file("src/Release-$s/libllama.a") }})
            onlyIf { ios.simulator.enabled.getOrElse(true) }
        }
        val buildIosDevice = project.tasks.register<LlamaCppIosBuildTask>("buildLlamaCppIosDevice") {
            group = "llama"
            description = "Builds static library for llama.cpp (iOS device)."
            llamaCppDir.set(ext.llamaCppDir.map { it.asFile })
            buildDir.set(ios.device.buildDir)
            sysroot.set(ios.device.sysroot)
            architectures.set(ios.device.architectures)
            deploymentTarget.set(ios.device.deploymentTarget)
            staticLib.set(buildDir.flatMap { dir -> sysroot.map { s -> dir.file("src/Release-$s/libllama.a") }})
            onlyIf { ios.device.enabled.getOrElse(true) }
        }

        // Desktop build
        val buildDesktop = project.tasks.register<LlamaCppDesktopBuildTask>("buildLlamaCppDesktop") {
            group = "llama"
            description = "Builds llama.cpp for desktop platform."
            llamaCppDir.set(ext.llamaCppDir.map { it.asFile })
            buildDir.set(ext.desktop.buildDir)
            enableMetal.set(ext.desktop.enableMetal)
            enableCuda.set(ext.desktop.enableCuda)
            enableOpenMP.set(ext.desktop.enableOpenMP)
            enableBlas.set(ext.desktop.enableBlas)
            enableVulkan.set(ext.desktop.enableVulkan)
            architectures.set(ext.desktop.architectures)
            macOsSharedLibs.set(ext.desktop.macOsSharedLibs)
            val extension = when (currentBuildPlatform) {
                BuildPlatform.MacOS -> "dylib"
                BuildPlatform.Linux -> "so"
                BuildPlatform.Windows -> "dll"
                else -> throw IllegalStateException("Unsupported operating system")
            }
            staticLib.set(buildDir.map { dir -> dir.file("bin/libllama.$extension") })

            onlyIf { ext.desktop.enabled.getOrElse(true) }
        }

        // Android build
        val buildAndroid = project.tasks.register<LlamaCppAndroidBuildTask>("buildLlamaCppAndroid") {
            group = "llama"
            description = "Builds llama.cpp for Android platform."
            llamaCppDir.set(ext.llamaCppDir.map { it.asFile })
            buildDir.set(android.buildDir)
            ndkDirectory.set(android.ndkDirectory)
            androidPlatform.set(android.androidPlatform)
            abi.set(android.abi)
            hostArch.set(android.hostArch)
            gpuType.set(android.gpuType)
            staticLib.set(buildDir.map { dir -> dir.file("src/libllama.a") })

            onlyIf { android.enabled.getOrElse(true) }
        }

        // If OpenCL GPU selected, prepare ICD loader build to be included in copy per-variant
        val selectedGpu = android.gpuType.get()
        var openClBuild: org.gradle.api.tasks.TaskProvider<OpenClIcdLoaderBuildTask>? = null
        var icdBuildDirProvider: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>? = null
        if (selectedGpu == AndroidGpu.OpenCl) {
            val openClAbi = android.abi.get()
            val icdBuildDir = project.layout.buildDirectory.dir("opencl/icd-loader/${openClAbi}")
            icdBuildDirProvider = icdBuildDir
            openClBuild = project.tasks.register<OpenClIcdLoaderBuildTask>("buildOpenClIcdLoaderFor$openClAbi") {
                group = "opencl"
                abi.set(openClAbi)
                ndkDir.set(project.layout.dir(project.provider { android.ndkDirectory.get() }))
                openClHeadersDir.set(project.layout.projectDirectory.dir("native/OpenCL-Headers"))
                openClIcdSrcDir.set(project.layout.projectDirectory.dir("native/OpenCL-ICD-Loader"))
                openClBuildDir.set(icdBuildDir)
            }
        }

        // Hook desktop Kotlin compilation to depend on native build
        project.tasks.matching { it.name.contains("cinteropLlamaMacosArm64") }.configureEach { dependsOn(buildDesktop) }
        project.tasks.matching { it.name.contains("cinteropLlamaLinuxX64") }.configureEach { dependsOn(buildDesktop) }
        project.tasks.matching { it.name.contains("cinteropLlamaMingwX64") }.configureEach { dependsOn(buildDesktop) }
        project.tasks.matching { it.name.contains("cinteropLlamaAndroidNativeArm64") }.configureEach { dependsOn(buildAndroid) }
        // Hook iOS cinterop tasks to native builds by name to avoid hard plugin deps
        project.tasks.matching { it.name.startsWith("cinteropLlamaIosX64") || it.name.startsWith("cinteropLlamaIosSimulatorArm64") }
            .configureEach { dependsOn(buildIosSimulator) }
        project.tasks.matching { it.name.startsWith("cinteropLlamaIosArm64") }
            .configureEach {
                dependsOn(buildIosDevice)
                onlyIf { ios.device.enabled.getOrElse(true) }
            }

        // Hook Android Kotlin/Native link tasks: register per-variant copy tasks after all tasks are configured
        project.afterEvaluate {
            val abiValue = android.abi.get()
            val hostArch = android.hostArch.get()
            val hostPlatform = when (currentBuildPlatform) {
                BuildPlatform.MacOS -> "darwin"
                BuildPlatform.Linux -> "linux"
                BuildPlatform.Windows -> "windows"
                else -> throw IllegalStateException("Unsupported operating system")
            }
            val ndkRoot = android.ndkDirectory.get().absolutePath

            project.tasks
                .matching { it.name.startsWith("link") && it.name.endsWith("SharedAndroidNativeArm64") }
                .forEach { linkTask ->
                    val cap = linkTask.name.removePrefix("link").removeSuffix("SharedAndroidNativeArm64")
                    val linkProvider = project.tasks.named(linkTask.name)

                    val copyFat = project.tasks.register<Copy>("copyFatLlamaSo$cap") {
                        dependsOn(buildAndroid, linkProvider)
                        into(project.layout.buildDirectory.dir("generated/jniLibs/$cap/$abiValue").get().asFile)
                        from(project.provider { linkProvider.get().outputs.files })
                        from(File(ndkRoot, "toolchains/llvm/prebuilt/$hostPlatform-$hostArch/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so"))
                        from(project.layout.projectDirectory.dir("native/llama.cpp/build-android/bin")) {
                            include("*.so")
                        }
                        if (selectedGpu == AndroidGpu.OpenCl) {
                            openClBuild?.let { dependsOn(it) }
                            icdBuildDirProvider?.let { provider ->
                                from(provider.get().file("libOpenCL.so").asFile)
                            }
                        }
                    }

                    project.tasks.matching { task -> task.name == "merge${cap}JniLibFolders" || task.name == "merge${cap}NativeLibs" }
                        .configureEach { dependsOn(copyFat) }
                }
        }

        // Clean task for native outputs
        val cleanNative = project.tasks.register<Delete>("cleanNative") {
            group = "llama"
            description = "Clean native build artifacts"
            delete(
                ext.desktop.buildDir.get().asFile,
                ios.simulator.buildDir.get().asFile,
                ios.device.buildDir.get().asFile,
                android.buildDir.get().asFile
            )
        }
        project.tasks.matching { task -> task.name == "clean" }.configureEach { dependsOn(cleanNative) }
    }
}

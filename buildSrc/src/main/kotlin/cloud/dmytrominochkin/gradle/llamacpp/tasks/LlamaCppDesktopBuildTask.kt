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
package cloud.dmytrominochkin.gradle.llamacpp.tasks

import cloud.dmytrominochkin.gradle.BuildPlatform
import cloud.dmytrominochkin.gradle.currentBuildPlatform
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class LlamaCppDesktopBuildTask @Inject constructor(
    execOperations: ExecOperations
) : LlamaCppBaseBuildTask(execOperations) {

    @get:Input
    abstract val enableMetal: Property<Boolean>

    @get:Input
    abstract val enableCuda: Property<Boolean>

    @get:Input
    abstract val enableOpenMP: Property<Boolean>

    @get:Input
    abstract val enableBlas: Property<Boolean>

    @get:Input
    abstract val enableVulkan: Property<Boolean>

    @get:Input
    abstract val architectures: Property<String>

    @get:Input
    abstract val macOsSharedLibs: Property<Boolean>

    override fun configureCmakeArguments(cmakeArgs: MutableList<String>) {
        // Platform-specific configurations
        when (currentBuildPlatform) {
            BuildPlatform.MacOS -> {
                cmakeArgs.addAll(
                    listOf(
                        "-DBUILD_SHARED_LIBS=${if (macOsSharedLibs.get()) "ON" else "OFF"}",
                        "-DCMAKE_OSX_ARCHITECTURES=${architectures.get()}",
                        "-DGGML_METAL=${if (enableMetal.get()) "ON" else "OFF"}",
                        "-DGGML_METAL_EMBED_LIBRARY=${if (enableMetal.get()) "ON" else "OFF"}",
                        "-DGGML_BLAS_DEFAULT=${if (enableBlas.get()) "ON" else "OFF"}",
                        "-DGGML_OPENMP=${if (enableOpenMP.get()) "ON" else "OFF"}",
                    )
                )
                if (enableOpenMP.get()) {
                    cmakeArgs.addAll(
                        listOf(
                            "-DLIBOMP_LIBRARY=/opt/homebrew/opt/libomp/lib/libomp.dylib",
                            "-DLIBOMP_INCLUDE_DIR=/opt/homebrew/opt/libomp/include"
                        )
                    )
                }
            }

            BuildPlatform.Linux -> {
                cmakeArgs.addAll(
                    listOf(
                        "-DBUILD_SHARED_LIBS=ON",
                        "-DGGML_CUDA=${if (enableCuda.get()) "ON" else "OFF"}",
                        "-DGGML_BLAS_DEFAULT=${if (enableBlas.get()) "ON" else "OFF"}",
                        "-DGGML_OPENMP=${if (enableOpenMP.get()) "ON" else "OFF"}",
                        "-DGGML_VULKAN=${if (enableVulkan.get()) "ON" else "OFF"}",
                    )
                )
            }

            BuildPlatform.Windows -> {
                cmakeArgs.addAll(
                    listOf(
                        "-DBUILD_SHARED_LIBS=ON",
                        // I had no luck with dynamic backends on windows
                        // "-DGGML_BACKEND_DL=ON",
                        // Can't compile Cuda with Mingw, only MSVC. Force off
                        "-DGGML_CUDA=OFF",
                        "-DGGML_BLAS_DEFAULT=${if (enableBlas.get()) "ON" else "OFF"}",
                        "-DGGML_OPENMP=${if (enableOpenMP.get()) "ON" else "OFF"}",
                        "-DGGML_VULKAN=${if (enableVulkan.get()) "ON" else "OFF"}",
                    )
                )
            }

            else -> {
                throw IllegalStateException("Unsupported operating system")
            }
        }
    }
} 

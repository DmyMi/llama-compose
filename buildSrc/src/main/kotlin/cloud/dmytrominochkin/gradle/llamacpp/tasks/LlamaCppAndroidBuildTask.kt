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

import cloud.dmytrominochkin.gradle.AndroidGpu
import cloud.dmytrominochkin.gradle.BuildPlatform
import cloud.dmytrominochkin.gradle.currentBuildPlatform
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class LlamaCppAndroidBuildTask @Inject constructor(
    execOperations: ExecOperations
) : LlamaCppBaseBuildTask(execOperations) {

    @get:Input
    abstract val ndkDirectory: Property<File>

    @get:Input
    abstract val androidPlatform: Property<String>

    @get:Input
    abstract val abi: Property<String>

    @get:Input
    abstract val gpuType: Property<AndroidGpu>

    @get:Input
    abstract val hostArch: Property<String>

    override fun configureCmakeArguments(cmakeArgs: MutableList<String>) {
        val osArch = hostArch.get()

        val ndkPlatform = when (currentBuildPlatform) {
            BuildPlatform.MacOS -> "darwin"
            BuildPlatform.Linux -> "linux"
            BuildPlatform.Windows -> "windows"
            else -> throw IllegalStateException("Unsupported operating system")
        }

        cmakeArgs.addAll(
            listOf(
                "-DCMAKE_TOOLCHAIN_FILE=${ndkDirectory.get().absolutePath}/build/cmake/android.toolchain.cmake",
                "-DGGML_LLAMAFILE=OFF",
                "-DGGML_OPENMP=OFF",
                "-DBUILD_SHARED_LIBS=ON",
                "-DANDROID_PLATFORM=${androidPlatform.get()}",
                "-DANDROID_ABI=${abi.get()}",
                "-DCMAKE_SYSROOT=${ndkDirectory.get().absolutePath}/toolchains/llvm/prebuilt/$ndkPlatform-$osArch/sysroot"
            )
        )

        val selectedGpu = gpuType.get()
        val nativeDir = llamaCppDir.get().parentFile
        when (selectedGpu) {
            AndroidGpu.Vulkan -> {
                val apiLevel = androidPlatform.get().removePrefix("android-")
                val vulkanIncludeDir = File(nativeDir, "Vulkan-Headers/include").absolutePath
                val ndkRoot = ndkDirectory.get().absolutePath
                val vulkanLib = "$ndkRoot/toolchains/llvm/prebuilt/$ndkPlatform-$osArch/sysroot/usr/lib/aarch64-linux-android/$apiLevel/libvulkan.so"
                val glslc = "$ndkRoot/shader-tools/$ndkPlatform-$osArch/glslc"
                cmakeArgs.addAll(
                    listOf(
                        "-DGGML_VULKAN=ON",
                        "-DVulkan_INCLUDE_DIR=$vulkanIncludeDir",
                        "-DVulkan_LIBRARY=$vulkanLib",
                        "-DVulkan_GLSLC_EXECUTABLE=$glslc",
                        "-DGGML_VULKAN_RUN_TESTS=OFF",
                    )
                )
            }
            AndroidGpu.OpenCl -> {
                val openClIncludeDir = File(nativeDir, "OpenCL-Headers").absolutePath
                val openClSo = File(project.layout.buildDirectory.dir("opencl/icd-loader/${abi.get()}").get().asFile, "libOpenCL.so").absolutePath
                cmakeArgs.addAll(
                    listOf(
                        "-DGGML_OPENCL=ON",
                        "-DGGML_OPENCL_SMALL_ALLOC=ON",
                        "-DGGML_OPENCL_EMBED_KERNELS=ON",
                        // Uncomment if you plan to run on Adreno
//                    "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON",
                        "-DOpenCL_INCLUDE_DIR=$openClIncludeDir",
                        "-DOpenCL_LIBRARY=$openClSo",
                    )
                )
            }
            else ->  {
                // No additional config needed
            }
        }
    }
}

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
package cloud.dmytrominochkin.gradle.opencl.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.enterprise.test.FileProperty
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class OpenClIcdLoaderBuildTask @Inject constructor(
    @Internal val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    abstract val abi: Property<String>

    @get:InputDirectory
    abstract val ndkDir: DirectoryProperty

    @get:Input
    abstract val androidPlatform: Property<String>
    @get:InputDirectory
    abstract val openClHeadersDir: DirectoryProperty

    @get:InputDirectory
    abstract val openClIcdSrcDir: DirectoryProperty

    @get:OutputDirectory
    abstract val openClBuildDir: DirectoryProperty

    @TaskAction
    fun build() {
        val openClAbi = abi.get()
        val cmakeToolChain = File(ndkDir.get().asFile, "build/cmake/android.toolchain.cmake").absolutePath

        val headersPath = openClHeadersDir.get().asFile.absolutePath
        val icdSrcPath = openClIcdSrcDir.get().asFile.absolutePath
        val buildDirPath = openClBuildDir.get().asFile.absolutePath

        File(buildDirPath).mkdirs()

        execOperations.exec {
            workingDir = File(buildDirPath)
            commandLine(
                "cmake",
                "-S", icdSrcPath,
                "-B", buildDirPath,
                "-G", "Ninja",
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_TOOLCHAIN_FILE=${cmakeToolChain}",
                "-DANDROID_ABI=$openClAbi",
                "-DANDROID_PLATFORM=${androidPlatform.get().removePrefix("android-")}",
                "-DANDROID_STL=c++_shared",
                "-DOPENCL_ICD_LOADER_HEADERS_DIR=$headersPath"
            )
        }

        execOperations.exec {
            commandLine("cmake", "--build", buildDirPath, "--target", "OpenCL")
        }
    }
}



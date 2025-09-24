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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File

abstract class LlamaCppBaseBuildTask(
    @Internal val execOperations: ExecOperations
) : DefaultTask() {

    @get:Internal
    abstract val buildDir: DirectoryProperty

    @get:OutputFile
    abstract val staticLib: RegularFileProperty

    @get:Input
    abstract val llamaCppDir: Property<File>

    abstract fun configureCmakeArguments(cmakeArgs: MutableList<String>)

    @TaskAction
    fun build() {
        val buildDirFile = buildDir.get().asFile

        val cmakeArgs = mutableListOf(
            "cmake",
            "-S", ".",
            "-B", buildDirFile.name,
            // Common configuration
            "-DCMAKE_BUILD_TYPE=Release",
            "-DLLAMA_BUILD_TOOLS=OFF",
            "-DLLAMA_BUILD_TESTS=OFF",
            "-DLLAMA_BUILD_EXAMPLES=OFF",
            "-DLLAMA_BUILD_SERVER=OFF",
            "-DLLAMA_INSTALL=OFF",
            "-DLLAMA_BUILD_COMMON=OFF",
            "-DLLAMA_CURL=OFF",
            "-DGGML_CCACHE=OFF",
            // makes build portable
            "-DGGML_NATIVE=OFF",
            ).apply(::configureCmakeArguments)

        execOperations.exec {
            workingDir = llamaCppDir.get()
            commandLine(cmakeArgs)
        }

        execOperations.exec {
            workingDir = llamaCppDir.get()
            commandLine("cmake", "--build", buildDirFile.absolutePath, "--config", "Release")
        }
    }
}

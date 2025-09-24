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

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class LlamaCppIosBuildTask @Inject constructor(
    execOperations: ExecOperations
) : LlamaCppBaseBuildTask(execOperations) {

    @get:Input
    abstract val sysroot: Property<String>

    @get:Input
    abstract val architectures: Property<String>

    @get:Input
    abstract val deploymentTarget: Property<String>

    override fun configureCmakeArguments(cmakeArgs: MutableList<String>) {
        cmakeArgs.addAll(
            listOf(
                "-G", "Xcode",
                "-DCMAKE_SYSTEM_NAME=iOS",
                "-DCMAKE_OSX_SYSROOT=${sysroot.get()}",
                "-DCMAKE_OSX_ARCHITECTURES=${architectures.get()}",
                "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTarget.get()}",
                "-DGGML_METAL=ON",
                "-DGGML_METAL_EMBED_LIBRARY=ON",
                "-DGGML_OPENMP=OFF",
                "-DBUILD_SHARED_LIBS=OFF",
            )
        )
    }
} 

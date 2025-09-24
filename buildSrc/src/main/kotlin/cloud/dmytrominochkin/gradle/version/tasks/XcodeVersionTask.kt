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
package cloud.dmytrominochkin.gradle.version.tasks

import cloud.dmytrominochkin.gradle.version.AppVersion
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class XcodeVersionTask() : DefaultTask() {
    @get:Input
    abstract val xcodeProject: Property<String>

    @get:Input
    internal abstract val versionString: Property<String>

    @get:Input
    internal abstract val bundleVersion: Property<Int>

    @get:OutputFile
    internal abstract val configFile: RegularFileProperty

    init {
        versionString.convention(project.providers.provider {
            (project.version as AppVersion).toString()
        })
        bundleVersion.convention(project.providers.provider {
            (project.version as AppVersion).getNumericAppVersion()
        })
        configFile.convention(
            project.rootProject.layout.projectDirectory.file(
                xcodeProject.map { projectDirName ->
                    "$projectDirName/Configuration/Version.xcconfig"
                }
            )
        )
    }

    @TaskAction
    fun setXcodeVersion() {
        val content = """
BUNDLE_VERSION_STRING = ${versionString.get()}
BUNDLE_VERSION = ${bundleVersion.get()}
    """.trimIndent()

        val outFile = configFile.get().asFile
        outFile.writeText(content)
    }
}

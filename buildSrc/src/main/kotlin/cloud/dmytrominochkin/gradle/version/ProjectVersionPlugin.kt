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
package cloud.dmytrominochkin.gradle.version

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*

@Suppress("unused")
abstract class ProjectVersionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val versionProps = Properties().apply {
            load(target.rootProject.file("version.properties").reader(Charsets.UTF_8))
        }
        target.project.version = getSanitizedVersionParts(
            versionProps.getProperty("APP_VERSION")
                ?: error("version property `APP_VERSION` not found in `version.properties`")
        )

        target.tasks.register("printVersion") {
            group = "versioning"
            description = "Prints the current version of the Project to stdout"
            val version = target.version
            doLast { println(version) }
        }

        target.tasks.register("printVersionCode") {
            group = "versioning"
            description = "Prints the current version code of the Project to stdout"
            val version = target.version as AppVersion
            doLast { println(version.getNumericAppVersion()) }
        }
    }
}

/**
 * Returns the major, minor, and patch version numbers of the app. The version numbers are taken
 * from the root project's version.properties
 */
private fun getSanitizedVersionParts(version: String): AppVersion {
    // Strip down everything after the first dash, e.g. "1.2.34-11" -> "1.2.34"
    val sanitizedVersionString = version.let { appVersionString ->
        appVersionString.indexOfOrNull('-')?.let { indexOfFirstDash ->
            appVersionString.take(indexOfFirstDash)
        } ?: appVersionString
    }

    return AppVersion.fromIdent(sanitizedVersionString)
}

private fun String.indexOfOrNull(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int? {
    val index = this.indexOf(char, startIndex, ignoreCase)
    return if (index == -1) null else index
}





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
package cloud.dmytrominochkin.ai.llamacompose

import cloud.dmytrominochkin.ai.llamacompose.llama.Platform
import cloud.dmytrominochkin.ai.llamacompose.llama.currentPlatform
import okio.Path
import okio.Path.Companion.toPath

/**
 * Desktop actual for [getUserDataDir].
 *
 * - Windows: `%APPDATA%` (roaming) or `%LOCALAPPDATA%` (local) with optional
 *   author segment and optional version subdirectory.
 * - macOS: `~/Library/Application Support/<appName>` with optional version.
 * - Linux/Unix: `~/.local/share/<appName>` with optional version.
 *
 * The directory is not created automatically.
 */
actual fun getUserDataDir(
    context: PlatformContext,
    appName: String,
    appVersion: String?,
    appAuthor: String?,
    roaming: Boolean
): Path {
    fun versionPath(appPath: Path): Path = if (appVersion != null) {
        appPath / appVersion
    } else {
        appPath
    }

    return when (currentPlatform) {
        Platform.Windows -> {
            val appData = if (roaming) {
                System.getenv("APPDATA")
            } else {
                System.getenv("LOCALAPPDATA")
            }

            val basePath = appData.toPath()

            val appPath = if (appAuthor != null) {
                basePath / appAuthor / appName
            } else {
                basePath / appName
            }

            versionPath(appPath)
        }

        Platform.MacOS -> {
            val homeDir = System.getProperty("user.home").toPath()
            val appPath = homeDir / "Library" / "Application Support" / appName
            versionPath(appPath)
        }

        else -> { // Linux and other Unix-like systems
            val homeDir = System.getProperty("user.home").toPath()

            val appPath = homeDir / ".local" / "share" / appName
            versionPath(appPath)
        }
    }
}

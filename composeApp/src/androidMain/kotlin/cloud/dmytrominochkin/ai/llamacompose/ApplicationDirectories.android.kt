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

import okio.Path
import okio.Path.Companion.toPath

/**
 * Android actual for [getUserDataDir].
 *
 * Returns `<context.filesDir>/<appName>` inside the app's private storage.
 * The directory is not created automatically.
 */
actual fun getUserDataDir(
    context: PlatformContext,
    appName: String,
    appVersion: String?,
    appAuthor: String?,
    roaming: Boolean
): Path {
    // Use internal app data directory on Android
    val baseDir = context.filesDir.absolutePath.toPath()
    return baseDir / appName
}
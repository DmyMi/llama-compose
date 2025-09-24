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

/**
 * Returns the per-user application data directory for the current platform.
 *
 * The location is platform-dependent and may vary by OS conventions and flags:
 * - Android: internal app files directory.
 * - iOS: user's Documents directory.
 * - Desktop (Windows/macOS/Linux): varies by OS; may include version/author.
 *
 * @param context Platform context anchor.
 * @param appName Application name used to form the directory path.
 * @param appVersion Optional version subdirectory to append when supported.
 * @param appAuthor Optional author/organization segment (used on Windows desktop).
 * @param roaming Prefer a roaming profile directory when supported (Windows desktop).
 * @return Path to a writable per-user app data directory (not created).
 */
expect fun getUserDataDir(
    context: PlatformContext,
    appName: String,
    appVersion: String? = null,
    appAuthor: String? = null,
    roaming: Boolean = false
): Path
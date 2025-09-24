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

/**
 * Platform-neutral context handle used by APIs that require a contextual anchor.
 *
 * - Android: actual is a typealias to `android.content.Context`.
 * - iOS/Desktop: actual is a stateless singleton exposed via `PlatformContext.INSTANCE`.
 *
 * Prefer passing an application-level context when possible (on Android), and
 * use the singleton variant on platforms that do not require a context object.
 */
expect abstract class PlatformContext
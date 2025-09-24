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
package cloud.dmytrominochkin.ai.llamacompose.download

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

/**
 * iOS actual for [getHttpClient].
 *
 * Uses the Darwin engine because Koog adds CIO by default, which is unsuitable
 * for iOS (Kotlin/Native). Darwin integrates with NSURLSession.
 */
actual fun getHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin, block)

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
package cloud.dmytrominochkin.ai.llamacompose.llama

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import llama.ggml_log_level
import llama.llama_log_set
import kotlin.experimental.ExperimentalNativeApi

/**
 * Mapping of ggml/llama native log levels to Kotlin enum for routing logs.
 */
@ExperimentalForeignApi
enum class GgmlLogLevel(val level: ggml_log_level) {
    NONE(0.toUInt()),
    DEBUG(1.toUInt()),
    INFO(2.toUInt()),
    WARN(3.toUInt()),
    ERROR(4.toUInt()),
    CONT(5.toUInt())
}

/** Converts a C ggml log level to [GgmlLogLevel]. */
@ExperimentalForeignApi
fun fromCLevel(level: ggml_log_level): GgmlLogLevel =
    GgmlLogLevel.entries.find { logLevel -> logLevel.level == level } ?: GgmlLogLevel.NONE

/**
 * Installs a bridge that forwards llama.cpp native logs to KotlinLogging.
 *
 * Use [install] once before using the backend; call [uninstall] to detach.
 * The bridge stores a [StableRef] to a logger so that native callbacks can
 * safely log messages across the FFI boundary.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal object LlamaNativeLogBridge {
    private var loggerRef: StableRef<KLogger>? = null

    fun install() {
        if (loggerRef != null) return
        val logger = KotlinLogging.logger("Llama.Native")
        val ref = StableRef.create(logger)
        llama_log_set(staticCFunction { level, text, userData ->
            val llamaLogger = userData?.asStableRef<KLogger>()?.get()
            val l = fromCLevel(level)
            when (l) {
                GgmlLogLevel.NONE -> { /* noop */ }
                GgmlLogLevel.DEBUG -> llamaLogger?.debug { text?.toKString()?.trim() }
                GgmlLogLevel.INFO -> llamaLogger?.info { text?.toKString()?.trim() }
                GgmlLogLevel.WARN -> llamaLogger?.warn { text?.toKString()?.trim() }
                GgmlLogLevel.ERROR -> llamaLogger?.error { text?.toKString()?.trim() }
                GgmlLogLevel.CONT -> { /* noop */ }
            }
        }, ref.asCPointer())
        loggerRef = ref
    }

    fun uninstall() {
        llama_log_set(null, null)
        loggerRef?.dispose()
        loggerRef = null
    }
}

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

import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

internal actual fun initializePlatformLlama(): PlatformLlama = PlatformLlamaJvm()

internal class PlatformLlamaJvm(): PlatformLlama {

    private val logger = logger()

    private val bridge: LlamaBridgeLibrary
    private var handle: Pointer? = null

    private val _json = Json {
        ignoreUnknownKeys = true
    }

    override val json
        get() = _json

    init {
        if (currentPlatform != Platform.Android) {
            val resourcesDir = System.getProperty("compose.application.resources.dir")
            if (!resourcesDir.isNullOrBlank()) {
                System.setProperty("jna.library.path", resourcesDir)
            }
            bridge = try {
                Native.load("fatllama", LlamaBridgeLibrary::class.java)
            } catch (_: UnsatisfiedLinkError) {
                // Fallback: preload a safe, explicit set of known dependencies then retry
                val dir = resourcesDir?.let { File(it) }
                if (dir != null && dir.isDirectory) {
                    preloadKnownDependencies(dir, currentPlatform)
                }
                Native.load("fatllama", LlamaBridgeLibrary::class.java)
            }
        } else {
            bridge = Native.load("fatllama", LlamaBridgeLibrary::class.java)
        }
        handle = bridge.llk_new()
        logger.info { "Llama initialized" }
    }

    // Sometimes JNA/JNI may not load libs on the first try, so we help them
    @Suppress("UnsafeDynamicallyLoadedCode")
    private fun preloadKnownDependencies(dir: File, platform: Platform) {
        val names: List<String> = when (platform) {
            Platform.MacOS -> listOf(
                "libggml.dylib",
                "libggml-base.dylib",
                "libggml-cpu.dylib",
                "libggml-blas.dylib",
                "libggml-metal.dylib",
                "libllama.dylib",
            )
            Platform.Linux -> listOf(
                "libggml-base.so",
                "libggml.so",
                "libggml-cpu.so",
                "libggml-opencl.so",
                "libggml-vulkan.so",
                "libllama.so",
            )
            Platform.Windows -> listOf(
                "ggml-base.dll",
                "ggml.dll",
                "ggml-cpu.dll",
                "ggml-opencl.dll",
                "ggml-vulkan.dll",
                "libllama.dll",
            )
            else -> emptyList()
        }

        names.forEach { name ->
            val f = File(dir, name)
            if (f.isFile) {
                runCatching { System.load(f.absolutePath) }
            }
        }
    }

    override fun loadModel(modelPath: String, params: LlamaParams, samplingParams: LlamaSamplingParams): Boolean {
        val h = handle ?: run {
            handle = bridge.llk_new()
            handle
        } ?: return false
        return bridge.llk_load_model(
            h,
            modelPath,
            // Explicitly override value for JVM
            getProcessorCount(),
            params.nPredict,
            params.nCtx,
            params.useGpu,
            samplingParams.temperature,
            samplingParams.topK,
            samplingParams.topP,
            samplingParams.minP,
            samplingParams.repeatPenalty,
            samplingParams.frequencyPenalty,
            samplingParams.presencePenalty,
            samplingParams.seed,
            samplingParams.greedy,
            samplingParams.penalise
        )
    }

    override fun updateSamplingParams(samplingParams: LlamaSamplingParams): Boolean {
        val h = handle ?: return false
        return bridge.llk_update_sampling(
            h,
            samplingParams.temperature,
            samplingParams.topK,
            samplingParams.topP,
            samplingParams.minP,
            samplingParams.repeatPenalty,
            samplingParams.frequencyPenalty,
            samplingParams.presencePenalty,
            samplingParams.seed,
            samplingParams.greedy,
            samplingParams.penalise
        )
    }

    override fun generateText(prompt: String, maxTokens: Int): Flow<String> = callbackFlow {
        val h = handle
        if (h == null) {
            close()
            return@callbackFlow
        }
        val cb = LlamaBridgeLibrary.TextCallback { textChunk, _ ->
            trySendBlocking(textChunk)
        }
        val job = launch(Dispatchers.IO) {
            try {
                bridge.llk_generate_text(h, prompt, maxTokens, cb, Pointer.NULL)
            } finally {
                close()
            }
        }
        awaitClose {
            try {
                bridge.llk_request_cancel(h)
            } catch (_: Throwable) {
            } finally {
                job.cancel()
            }
        }
    }.buffer(Channel.UNLIMITED)

    override fun applyChatTemplate(messages: List<ChatMessage>, addAssistantPrompt: Boolean): String {
        val h = handle ?: return ""
        // Pass messages as JSON to keep ABI simple
        val serialized = json.encodeToString(messages)
        val ptr = bridge.llk_apply_chat_template(h, serialized, addAssistantPrompt)
        if (ptr == Pointer.NULL) return messages.lastOrNull { it.role == "user" }?.content ?: ""
        val result = ptr.getString(0)
        bridge.llk_free_cstr(ptr)
        return result
    }

    override fun clearConversationState() {
        handle?.let { bridge.llk_clear_state(it) }
    }

    override fun requestCancel() {
        handle?.let {
            runCatching { bridge.llk_request_cancel(it) }
        }
    }

    override fun unloadModel() {
        handle?.let { bridge.llk_unload_model(it) }
    }

    override val isModelLoaded: Boolean
        get() {
            val h = handle ?: return false
            return bridge.llk_is_model_loaded(h)
        }

    override fun freeBackend() {
        val h = handle ?: return
        bridge.llk_free_backend(h)
        handle = null
    }

    override fun getModelMetaValue(key: String): String? {
        val h = handle ?: return null
        val ptr = bridge.llk_get_meta(h, key)
        if (ptr == Pointer.NULL) return null
        val s = ptr.getString(0)
        bridge.llk_free_cstr(ptr)
        return s
    }

    override fun getContextUsagePercent(seqId: Int): Float {
        val h = handle ?: return 0f
        return bridge.llk_ctx_usage_percent(h, seqId)
    }

    override val supportsGpuOffloading: Boolean
        get() {
            val h = handle ?: return false
            return bridge.llk_supports_gpu(h)
        }
}

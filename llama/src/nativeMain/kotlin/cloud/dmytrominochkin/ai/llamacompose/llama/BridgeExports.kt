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
@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@file:Suppress("unused", "FunctionName")

package cloud.dmytrominochkin.ai.llamacompose.llama

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import platform.posix.free
import platform.posix.strdup
import kotlin.experimental.ExperimentalNativeApi

/**
 * Creates a new platform core and wraps it into a stable reference to be
 * passed across the C FFI boundary. The caller is responsible for disposing
 * the reference via [llk_free_backend].
 */
private fun newCore(): StableRef<PlatformLlama> = StableRef.create(initializePlatformLlama())


/**
 * Creates a new core instance and returns an opaque pointer to be used with
 * all other exported functions.
 */
@CName("llk_new")
fun llk_new(): COpaquePointer {
    return newCore().asCPointer()
}

/**
 * Unloads the model associated with the given handle. Safe if no model loaded.
 */
@CName("llk_unload_model")
fun llk_free(handle: COpaquePointer) {
    val ref = handle.asStableRef<PlatformLlama>()
    ref.get().unloadModel()
}

/**
 * Frees backend-wide resources for the given handle and disposes the stable reference.
 * Should be called once at application shutdown; do not reuse the handle afterwards.
 */
@CName("llk_free_backend")
fun llk_free_backend(handle: COpaquePointer) {
    val ref = handle.asStableRef<PlatformLlama>()
    try {
        ref.get().freeBackend()
    } catch (_: Throwable) {
    } finally {
        ref.dispose()
    }
}

/**
 * Loads a GGUF model and applies runtime and sampling parameters.
 * Returns true on success.
 */
@CName("llk_load_model")
fun llk_load_model(
    handle: COpaquePointer,
    modelPath: CPointer<ByteVar>,
    nThreads: Int,
    nPredict: Int,
    nCtx: Int,
    useGpu: Boolean,
    temperature: Float,
    topK: Int,
    topP: Float,
    minP: Float,
    repeatPenalty: Float,
    frequencyPenalty: Float,
    presencePenalty: Float,
    seed: Int,
    greedy: Boolean,
    penalise: Boolean
): Boolean {
    val core = handle.asStableRef<PlatformLlama>().get()
    return core.loadModel(
        modelPath.toKString(),
        LlamaParams(
            nThreads = nThreads,
            nPredict = nPredict,
            nCtx = nCtx,
            useGpu = useGpu
        ),
        LlamaSamplingParams(
            temperature = temperature,
            topK = topK,
            topP = topP,
            minP = minP,
            repeatPenalty = repeatPenalty,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            seed = seed,
            greedy = greedy,
            penalise = penalise
        )
    )
}

/** Updates sampling parameters for subsequent generation calls. */
@CName("llk_update_sampling")
fun llk_update_sampling(
    handle: COpaquePointer,
    temperature: Float,
    topK: Int,
    topP: Float,
    minP: Float,
    repeatPenalty: Float,
    frequencyPenalty: Float,
    presencePenalty: Float,
    seed: Int,
    greedy: Boolean,
    penalise: Boolean
): Boolean {
    val core = handle.asStableRef<PlatformLlama>().get()
    return core.updateSamplingParams(
        LlamaSamplingParams(
            temperature = temperature,
            topK = topK,
            topP = topP,
            minP = minP,
            repeatPenalty = repeatPenalty,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            seed = seed,
            greedy = greedy,
            penalise = penalise
        )
    )
}

/** Clears conversation/KV state without unloading the model. */
@CName("llk_clear_state")
fun llk_clear_state(handle: COpaquePointer) {
    handle.asStableRef<PlatformLlama>().get().clearConversationState()
}

/** Requests cooperative cancellation of an in-flight generation. */
@CName("llk_request_cancel")
fun llk_request_cancel(handle: COpaquePointer) {
    handle.asStableRef<PlatformLlama>().get().requestCancel()
}

/** True if a model is currently loaded for this handle. */
@CName("llk_is_model_loaded")
fun llk_is_model_loaded(handle: COpaquePointer): Boolean = handle.asStableRef<PlatformLlama>().get().isModelLoaded

/**
 * Returns a newly allocated C string for the metadata value or null if not found.
 * The returned string must be freed via [llk_free_cstr].
 */
@CName("llk_get_meta")
fun llk_get_meta(handle: COpaquePointer, key: CPointer<ByteVar>): CPointer<ByteVar>? {
    val v = handle.asStableRef<PlatformLlama>().get().getModelMetaValue(key.toKString()) ?: return null
    return strdup(v)?.reinterpret()
}

/** Frees a C string returned by functions in this bridge. */
@CName("llk_free_cstr")
fun llk_free_cstr(ptr: CPointer<ByteVar>?) {
    if (ptr != null) free(ptr)
}

/** Percentage (0..100) of KV cache used for a given sequence id. */
@CName("llk_ctx_usage_percent")
fun llk_ctx_usage_percent(handle: COpaquePointer, seqId: Int): Float =
    handle.asStableRef<PlatformLlama>().get().getContextUsagePercent(seqId)

/**
 * Applies chat template to messages JSON. Returns a newly allocated C string
 * that must be freed via [llk_free_cstr]. If [addAssistant] is true, the
 * assistant prefix is appended to enable continuation.
 */
@CName("llk_apply_chat_template")
fun llk_apply_chat_template(
    handle: COpaquePointer,
    messagesJson: CPointer<ByteVar>,
    addAssistant: Boolean
): CPointer<ByteVar>? {
    val h = handle.asStableRef<PlatformLlama>().get()
    val serialized = messagesJson.toKString()
    val messages = h.json.decodeFromString<List<ChatMessage>>(serialized)
    val res = h.applyChatTemplate(messages, addAssistant)
    return strdup(res)?.reinterpret()
}

typealias TextCb = (CPointer<ByteVar>?, COpaquePointer?) -> Unit

/**
 * Generates text for the given prompt and streams chunks via callback.
 * Returns the number of chunks emitted.
 */
@CName("llk_generate_text")
fun llk_generate_text(
    handle: COpaquePointer,
    prompt: CPointer<ByteVar>,
    maxTokens: Int,
    cb: CPointer<CFunction<TextCb>>?,
    userData: COpaquePointer?
): Int {
    val core = handle.asStableRef<PlatformLlama>().get()
    if (cb == null) return 0
    var produced = 0
    val callback = cb.reinterpret<CFunction<TextCb>>()
    runBlocking {
        core.generateText(prompt.toKString(), maxTokens).collect { chunk ->
            produced++
            memScoped {
                val cstr = chunk.cstr.getPointer(memScope)
                callback(cstr, userData)
            }
        }
    }
    return produced
}

/** True if this build/runtime supports GPU offloading for this handle. */
@CName("llk_supports_gpu")
fun llk_supports_gpu(handle: COpaquePointer): Boolean {
    val h = handle.asStableRef<PlatformLlama>().get()
    return h.supportsGpuOffloading
}

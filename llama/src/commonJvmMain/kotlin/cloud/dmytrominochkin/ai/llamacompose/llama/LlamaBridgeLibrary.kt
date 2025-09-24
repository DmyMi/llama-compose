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

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Pointer

/**
 * JNA interface to the native llama.cpp bridge.
 *
 * Each function maps 1:1 to an exported C symbol defined in native `BridgeExports.kt`.
 * The handle returned by [llk_new] represents a native core instance and must be
 * passed to all subsequent calls. Use [llk_free_backend] once at shutdown to free
 * backend-wide resources.
 */
@Suppress("FunctionName")
internal interface LlamaBridgeLibrary : Library {
    /** Creates a new native core and returns an opaque handle. */
    fun llk_new(): Pointer
    /** Unloads the current model for the given handle. Safe if no model loaded. */
    fun llk_unload_model(handle: Pointer)

    /** Frees backend-wide resources and disposes the handle. Call once at shutdown. */
    fun llk_free_backend(handle: Pointer)

    /**
     * Loads a GGUF model for the given handle.
     * Returns true on success.
     */
    fun llk_load_model(
        handle: Pointer,
        modelPath: String,
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
    ): Boolean
    /** Updates sampling parameters for the given handle. Returns true if applied. */
    fun llk_update_sampling(
        handle: Pointer,
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
    ): Boolean

    /** Clears conversation/KV state without unloading the model. */
    fun llk_clear_state(handle: Pointer)
    /** Requests cooperative cancellation of an in-flight generation. */
    fun llk_request_cancel(handle: Pointer)
    /** True if a model is currently loaded for this handle. */
    fun llk_is_model_loaded(handle: Pointer): Boolean

    /**
     * Returns a pointer to a C string for the metadata value. Must be freed via [llk_free_cstr].
     * Returns null if key not found.
     */
    fun llk_get_meta(handle: Pointer, key: String): Pointer
    /** Frees a C string obtained from [llk_get_meta] or other functions returning C strings. */
    fun llk_free_cstr(ptr: Pointer)

    /** Percentage (0..100) of KV cache used for a sequence id. */
    fun llk_ctx_usage_percent(handle: Pointer, seqId: Int): Float

    /**
     * Applies chat template to messages JSON. Returns C string to be freed via [llk_free_cstr].
     */
    fun llk_apply_chat_template(handle: Pointer, messagesJson: String, addAssistant: Boolean): Pointer

    /**
     * Generates text and streams chunks via [TextCallback].
     * Returns the number of chunks emitted.
     */
    fun llk_generate_text(
        handle: Pointer,
        prompt: String,
        maxTokens: Int,
        cb: TextCallback?,
        userData: Pointer?
    ): Int

    /** True if this build/runtime supports GPU offloading for this handle. */
    fun llk_supports_gpu(handle: Pointer): Boolean

    fun interface TextCallback : Callback {
        /** Called for each generated text chunk. */
        fun invoke(textChunk: String, userData: Pointer?)
    }
}



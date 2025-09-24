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

import kotlinx.coroutines.flow.Flow

/**
 * High-level, platform-agnostic API for working with a local LLM.
 * Implementations delegate to platform-specific backends while exposing
 * a coroutine-friendly, streaming interface for text and chat generation.
 *
 * Lifecycle:
 * - Call [setModelParams] to adjust runtime defaults before loading a model.
 * - Call [loadModel] to load a model from a GGUF path.
 * - Use [generateText], [generateChat] or [continueConversation] to produce tokens.
 * - Call [requestCancel] to cooperatively stop an in-flight generation.
 * - Call [unloadModel] to free model resources, and [freeBackend] once on app shutdown.
 */
interface LlamaModel {
    /**
     * Loads a model from the provided file path.
     *
     * @param modelPath Absolute or normalized path to a GGUF model file.
     * @param samplingParams Initial sampling configuration to apply after load.
     * @return true if the model loads successfully, false otherwise.
     *
     * Notes:
     * - If a model is already loaded, implementations may unload and replace it.
     * - Global backend resources should be released via [freeBackend] on shutdown.
     */
    suspend fun loadModel(modelPath: String, samplingParams: LlamaSamplingParams = LlamaSamplingParams()): Boolean
    /**
     * Generates text for the given prompt as a stateless call.
     *
     * @param prompt The input prompt.
     * @param maxTokens Upper bound on the number of tokens to emit.
     * @return A [Flow] emitting incremental text chunks until completion.
     *
     * Cancellation: cancel the collecting coroutine or call [requestCancel].
     */
    suspend fun generateText(prompt: String, maxTokens: Int = 100): Flow<String>
    /**
     * Generates the assistant response for a multi-turn chat.
     *
     * Implementations may apply a model-specific chat template if available.
     *
     * @param messages Conversation history including system, user and assistant messages.
     * @param maxTokens Upper bound on the number of tokens to emit.
     * @return A [Flow] emitting incremental text chunks until completion.
     */
    suspend fun generateChat(messages: List<ChatMessage>, maxTokens: Int = 100): Flow<String>
    /**
     * Unloads the currently loaded model (if any) and frees model-scoped resources.
     * Safe to call when no model is loaded.
     */
    suspend fun unloadModel()
    /** Indicates whether a model is currently loaded and ready to generate. */
    val isModelLoaded: Boolean
    /**
     * Get model metadata value
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    suspend fun getModelMetaValue(key: String): String?

    /** Returns the detected model family/type for the currently loaded model. */
    suspend fun getLoadedModelType(): Models

    /**
     * Whether the active backend can offload computation to a GPU on this device.
     * This capability may be independent of the current model load state.
     */
    val supportsGpuOffloading: Boolean

    /**
     * Updates runtime parameters for subsequent loads/generations (e.g., threads, context length, GPU preference).
     * Some parameters apply immediately; others may take effect on the next [loadModel] call.
     */
    fun setModelParams(params: LlamaParams)

    /** Returns the currently configured runtime parameters. */
    fun getModelParams(): LlamaParams

    /**
     * Starts a new, clean chat session, clearing all internal memory in the native library.
     */
    fun clearConversation()

    /**
     * Request cooperative cancellation of any in-flight generation.
     */
    fun requestCancel()

    /**
     * Generates the next turn in the conversation based on the user's message.
     * The native library manages conversation history internally via KV cache.
     * Only the new user message needs to be passed to the lower level library.
     */
    suspend fun continueConversation(userMessage: String, maxTokens: Int = 100): Flow<String>

    /**
     * Update the sampling parameters dynamically without reloading the model.
     */
    suspend fun updateSamplingParams(samplingParams: LlamaSamplingParams): Boolean

    /**
     * Returns KV-cache usage percentage for the given sequence id.
     * @param seqId The sequence ID (defaults to 0)
     * @return Percentage (0.0..100.0) of KV cache used
     */
    suspend fun getContextUsagePercent(seqId: Int = 0): Float

    /**
     * Free the backend resources. Should be called once on application termination.
     * This is separate from unloadModel() which only frees model-specific resources.
     */
    fun freeBackend()
}

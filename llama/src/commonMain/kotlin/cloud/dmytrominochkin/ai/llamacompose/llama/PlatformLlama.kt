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
import kotlinx.serialization.json.Json

/**
 * Creates the platform-specific implementation of [PlatformLlama].
 *
 * On each target (Android/JVM, iOS, Desktop, etc.) this function returns an
 * instance wired to the native llama.cpp bindings and platform facilities.
 */
internal expect fun initializePlatformLlama(): PlatformLlama

/**
 * Low-level platform bridge over the native llama backend.
 *
 * This interface provides a minimal, allocation-conscious API that closely
 * mirrors the C/C++ layer. Higher-level code should prefer [LlamaModel].
 *
 * Implementations must be thread-safe where applicable and promptly honor
 * cancellation via [requestCancel].
 */
internal interface PlatformLlama {
    /** JSON instance used to serialize/deserialize message lists for templating. */
    val json: Json
    /**
     * Loads a model from disk and applies the provided parameters.
     *
     * @param modelPath Filesystem path to a GGUF model file.
     * @param params Backend/runtime parameters (threads, context length, GPU).
     * @param samplingParams Initial sampling configuration.
     * @return true if successful; false otherwise.
     */
    fun loadModel(modelPath: String, params: LlamaParams, samplingParams: LlamaSamplingParams): Boolean
    /**
     * Updates sampling parameters for subsequent generation calls.
     * Returns true if the update was applied.
     */
    fun updateSamplingParams(samplingParams: LlamaSamplingParams): Boolean
    /**
     * Generates text tokens for the given prompt and streams text chunks.
     * Cancel by cancelling the collecting coroutine or via [requestCancel].
     */
    fun generateText(prompt: String, maxTokens: Int): Flow<String>
    /**
     * Applies a model-specific chat template to the given messages.
     * If [addAssistantPrompt] is true, appends the assistant prefix for continuation.
     */
    fun applyChatTemplate(messages: List<ChatMessage>, addAssistantPrompt: Boolean): String
    /**
     * Request cooperative cancellation of the currently running generation if any.
     * Implementations should exit generation loops promptly when requested.
     */
    fun requestCancel()
    /** Clears any internal conversation/KV-cache state without unloading the model. */
    fun clearConversationState()
    /** Unloads the current model, freeing model-scoped resources. */
    fun unloadModel()
    /** Indicates whether a model is currently loaded. */
    val isModelLoaded: Boolean
    /** Frees backend-wide resources. Should be called once at application shutdown. */
    fun freeBackend()
    /**
     * Get model metadata value as string
     * @param key The metadata key (e.g., "general.name", "general.architecture", etc.)
     * @return The metadata value as string, or null if not found
     */
    fun getModelMetaValue(key: String): String?

    /**
     * Returns the percentage (0.0..100.0) of the KV cache used for the given sequence id
     * relative to the current context length.
     * If the KV cache is empty, returns 0.0.
     */
    fun getContextUsagePercent(seqId: Int = 0): Float

    /** True if this platform/backend supports GPU offloading for the current build. */
    val supportsGpuOffloading: Boolean
}

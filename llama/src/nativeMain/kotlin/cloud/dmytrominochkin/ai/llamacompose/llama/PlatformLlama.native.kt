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

import cnames.structs.llama_context
import cnames.structs.llama_model
import cnames.structs.llama_vocab
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.copy
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import llama.ggml_backend_buft_name
import llama.ggml_backend_dev_backend_reg
import llama.ggml_backend_dev_buffer_type
import llama.ggml_backend_dev_count
import llama.ggml_backend_dev_description
import llama.ggml_backend_dev_get
import llama.ggml_backend_dev_get_props
import llama.ggml_backend_dev_name
import llama.ggml_backend_dev_props
import llama.ggml_backend_dev_t
import llama.ggml_backend_dev_tVar
import llama.ggml_backend_dev_type
import llama.ggml_backend_reg_name
import llama.llama_backend_free
import llama.llama_backend_init
import llama.llama_batch_get_one
import llama.llama_chat_apply_template
import llama.llama_chat_message
import llama.llama_context_default_params
import llama.llama_decode
import llama.llama_free
import llama.llama_get_memory
import llama.llama_init_from_model
import llama.llama_memory_clear
import llama.llama_memory_seq_pos_max
import llama.llama_model_chat_template
import llama.llama_model_default_params
import llama.llama_model_free
import llama.llama_model_get_vocab
import llama.llama_model_load_from_file
import llama.llama_model_meta_val_str
import llama.llama_n_ctx
import llama.llama_sampler
import llama.llama_sampler_accept
import llama.llama_sampler_chain_add
import llama.llama_sampler_chain_default_params
import llama.llama_sampler_chain_init
import llama.llama_sampler_free
import llama.llama_sampler_init_dist
import llama.llama_sampler_init_greedy
import llama.llama_sampler_init_min_p
import llama.llama_sampler_init_penalties
import llama.llama_sampler_init_temp
import llama.llama_sampler_init_top_k
import llama.llama_sampler_init_top_p
import llama.llama_sampler_reset
import llama.llama_sampler_sample
import llama.llama_supports_gpu_offload
import llama.llama_token
import llama.llama_token_to_piece
import llama.llama_tokenize
import llama.llama_vocab_eos
import llama.llama_vocab_is_eog
import kotlin.experimental.ExperimentalNativeApi

internal actual fun initializePlatformLlama(): PlatformLlama = PlatformLlamaNative()

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal class PlatformLlamaNative() : PlatformLlama {

    private val logger = logger()

    private var model: CPointer<llama_model>? = null
    private var context: CPointer<llama_context>? = null
    private var vocab: CPointer<llama_vocab>? = null
    private var sampler: CPointer<llama_sampler>? = null
    private var isLoaded: Boolean = false
    private var cancelRequested: Boolean = false

    private val _json = Json {
        ignoreUnknownKeys = true
    }

    override val json
        get() = _json

    init {
        LlamaNativeLogBridge.install()
        logger.info { "Loading embedded backends" }
        llama_backend_init()
    }

    override fun loadModel(modelPath: String, params: LlamaParams, samplingParams: LlamaSamplingParams): Boolean {
        try {
            // Free any existing model first
            if (isLoaded) {
                unloadModel()
            }

            val defaultModelParams = llama_model_default_params()
            val wantGpu = params.useGpu
            val loadedModel = memScoped {
                val selected = selectPreferredGpuDevices(wantGpu, currentPlatform)
                logger.info { "Listing picked GPU devices..." }
                selected.filterNotNull().forEachIndexed(::logDevice)
                val devicesPtr: CPointer<ggml_backend_dev_tVar>? =
                    if (selected.isNotEmpty()) {
                        val arr = allocArray<ggml_backend_dev_tVar>(selected.size + 1)
                        for (i in selected.indices) arr[i] = selected[i]
                        arr[selected.size] = null
                        arr
                    } else null
                val modelParams = defaultModelParams.copy {
                    if (devicesPtr != null) {
                        devices = devicesPtr
                        n_gpu_layers = 99          // TODO: a user-tuned value later
                        main_gpu = 0               // first device in our list
                    } else {
                        n_gpu_layers = 0
                        main_gpu = -1
                    }
                    use_mmap = true // Enable memory mapping
                    use_mlock = false // Disable memory locking
                }
                logger.debug { "Setup model params" }
                llama_model_load_from_file(modelPath, modelParams)
            }

            if (loadedModel == null) return false

            model = loadedModel

            val maxCtxLength = getModelMetaValue("general.architecture")?.let { arch ->
                getModelMetaValue("$arch.context_length")?.toInt()
            } ?: params.nCtx

            // This is a sanity check, only smaller Gemmas can run with max ctx anyway, Llamas can eat up to 30+ GB RAM
            val finalCtx = if (params.nCtx <= maxCtxLength) params.nCtx else maxCtxLength

            logger.debug { "Using $finalCtx out of $maxCtxLength available" }

            val defaultContextParams = llama_context_default_params()
            val contextParams = defaultContextParams.copy {
                n_ctx = finalCtx.toUInt()
                n_batch = finalCtx.toUInt()
                n_threads = params.nThreads
                n_threads_batch = params.nThreads
            }

            logger.debug { "Setup context params" }
            val createdContext = llama_init_from_model(model, contextParams)
            if (createdContext == null) {
                llama_model_free(model)
                model = null
                return false
            }
            context = createdContext

            logger.debug { "Init vocab" }
            vocab = llama_model_get_vocab(model)

            logger.debug { "Setup sampler" }
            sampler = createSampler(samplingParams)

            logger.debug { "Model loaded" }

            isLoaded = true
            return true
        } catch (e: Exception) {
            logger.error(e) { "Error loading model" }
            return false
        }
    }

    private fun createSampler(samplingParams: LlamaSamplingParams): CPointer<llama_sampler>? {
        return if (samplingParams.greedy) {
            llama_sampler_init_greedy()
        } else {
            val samplerParams = llama_sampler_chain_default_params()
            llama_sampler_chain_init(samplerParams)?.also { chainPtr ->
                // Add samplers to chain (order matters: penalties -> top_k -> top_p -> temp -> dist)

                // Add penalties sampler if any penalty is enabled
                // TODO: Add penalties UI
                if (samplingParams.penalise) {
                    val penaltiesSampler = llama_sampler_init_penalties(
                        64, // penalty_last_n: consider last 64 tokens for penalties
                        samplingParams.repeatPenalty, // penalty_repeat
                        samplingParams.frequencyPenalty, // penalty_freq
                        samplingParams.presencePenalty  // penalty_present
                    )
                    llama_sampler_chain_add(chainPtr, penaltiesSampler)
                }

                // Add top_k sampler
                if (samplingParams.topK > 0) {
                    val topKSampler = llama_sampler_init_top_k(samplingParams.topK)
                    llama_sampler_chain_add(chainPtr, topKSampler)
                }

                // Add top_p sampler
                if (samplingParams.topP < 1.0f) {
                    val topPSampler = llama_sampler_init_top_p(samplingParams.topP, 1.convert())
                    llama_sampler_chain_add(chainPtr, topPSampler)
                }

                // Add min_p sampler
                if (samplingParams.minP > 0.0f) {
                    val minPSampler = llama_sampler_init_min_p(samplingParams.minP, 1.convert())
                    llama_sampler_chain_add(chainPtr, minPSampler)
                }

                // Add temperature sampler
                val tempSampler = llama_sampler_init_temp(samplingParams.temperature)
                llama_sampler_chain_add(chainPtr, tempSampler)

                // Add distribution sampler (must be last)
                val distSampler = llama_sampler_init_dist(samplingParams.seed.toUInt())
                llama_sampler_chain_add(chainPtr, distSampler)
            }
        }
    }

    override fun updateSamplingParams(samplingParams: LlamaSamplingParams): Boolean {
        if (!isLoaded) return false

        // Free the old sampler
        sampler?.let { llama_sampler_free(it) }

        // Create new sampler with updated parameters
        sampler = createSampler(samplingParams)

        return sampler != null
    }

    override fun getContextUsagePercent(seqId: Int): Float {
        val ctxPtr = context ?: return 0f
        val mem = llama_get_memory(ctxPtr) ?: return 0f

        val nCtx = llama_n_ctx(ctxPtr).toInt()
        if (nCtx <= 0) return 0f

        val maxPos = llama_memory_seq_pos_max(mem, seqId)
        if (maxPos < 0) return 0f // empty KV cache

        val used = (maxPos + 1).toFloat()
        return (used * 100f / nCtx.toFloat()).coerceIn(0f, 100f)
    }

    override fun applyChatTemplate(messages: List<ChatMessage>, addAssistantPrompt: Boolean): String {
        if (!isLoaded) error("Model not loaded. Call loadModel() first.")

        val mdl = model ?: error("Model not loaded")
        memScoped {
            val cMessages = allocArray<llama_chat_message>(messages.size) { i ->
                val msg = messages[i]
                role = msg.role.cstr.getPointer(memScope)
                content = msg.content.cstr.getPointer(memScope)
            }

            val tmplPtr = llama_model_chat_template(mdl, null)
            if (tmplPtr == null) {
                logger.warn { "No chat template found, using last user message." }
                return messages.lastOrNull { it.role == "user" }?.content ?: ""
            }

            var bufSize = 4096
            var buffer = allocArray<ByteVar>(bufSize)
            var n = llama_chat_apply_template(
                tmplPtr.toKString(),
                cMessages,
                messages.size.convert(),
                addAssistantPrompt,
                buffer,
                bufSize
            )

            if (n < 0) {
                logger.warn { "Failed to apply chat template, using last user message." }
                return messages.lastOrNull { it.role == "user" }?.content ?: ""
            }

            if (n >= bufSize) {
                bufSize = n + 1
                buffer = allocArray(bufSize) // Re-alloc in the same scope
                n = llama_chat_apply_template(
                    tmplPtr.toKString(),
                    cMessages,
                    messages.size.convert(),
                    addAssistantPrompt,
                    buffer,
                    bufSize
                )
            }
            return buffer.readBytes(n).decodeToString()
        }
    }

    override fun generateText(prompt: String, maxTokens: Int): Flow<String> = flow {
        if (!isLoaded) return@flow
        val ctx = context ?: return@flow
        model ?: return@flow
        val vcb = vocab ?: return@flow
        val samplerPtr = sampler ?: return@flow

        val tempBytes = mutableListOf<Byte>()

        // Tokenize the prompt
        val tokensList = tokenize(prompt, true)
        val nCtx = llama_n_ctx(ctx)
        if (tokensList.size + maxTokens > nCtx.toInt()) {
            logger.warn { "Prompt is too long for the context size." }
            return@flow
        }

        memScoped {
            // Create a simple batch for the prompt using llama_batch_get_one
            val tokensArray = allocArray<IntVar>(tokensList.size) { i ->
                value = tokensList[i]
            }

            var batch = llama_batch_get_one(tokensArray, tokensList.size)
            var generated = 0
            var emittedEog = false

            // Process the prompt first
            if (llama_decode(ctx, batch) != 0) {
                logger.warn { "llama_decode() failed for prompt" }
                return@flow
            }

            // Generation loop - generate tokens one by one
            cancelRequested = false
            while (generated < maxTokens && !cancelRequested) {
                // Sample the next token using -1 for the last token in batch
                val newTokenId = llama_sampler_sample(samplerPtr, ctx, -1)

                // Check if it's end of generation
                val isEogToken = llama_vocab_is_eog(vocab, newTokenId)

                // Accept the token to update sampler internal state (only for non-EOG tokens)
                if (!isEogToken) {
                    llama_sampler_accept(samplerPtr, newTokenId)
                }

                // 1. Feed token back to KV cache
                val singleTokenArray = allocArray<IntVar>(1)
                singleTokenArray[0] = newTokenId
                batch = llama_batch_get_one(singleTokenArray, 1)

                if (llama_decode(ctx, batch) != 0) {
                    logger.warn { "Failed to evaluate token $newTokenId" }
                    break
                }

                // 2. Check if it's end of generation and break
                if (isEogToken) {
                    emittedEog = true
                    break
                }

                // 3. Convert token to bytes and attempt to emit text when decodable
                val piece = tokenToPiece(newTokenId)
                if (piece.isNotEmpty()) {
                    tempBytes.addAll(piece.asList())
                    try {
                        val decoded = tempBytes.toByteArray().decodeToString()
                        if (decoded.isNotEmpty()) emit(decoded)
                        tempBytes.clear()
                    } catch (_: Throwable) {
                        // incomplete UTF-8 sequence; wait for more bytes
                    }
                }

                generated++
            }

            // Ensure the KV cache is properly closed with EOG even on cancellation
            if (!emittedEog) {
                val eogId = llama_vocab_eos(vcb)
                if (eogId != -1) {
                    val eogTokenArray = allocArray<IntVar>(1)
                    eogTokenArray[0] = eogId
                    batch = llama_batch_get_one(eogTokenArray, 1)
                    llama_decode(ctx, batch)
                }
            }

            // Flush remaining bytes that might form a complete character.
            if (tempBytes.isNotEmpty()) {
                try {
                    val tail = tempBytes.toByteArray().decodeToString()
                    if (tail.isNotEmpty()) emit(tail)
                } catch (_: Throwable) { // CharacterCodingException
                    // Ignore if the final leftover bytes are still not a valid string.
                }
            }
        }
    }

    override fun requestCancel() {
        cancelRequested = true
    }

    override fun clearConversationState() {
        if (!isLoaded) return
        context?.let { ctx ->
            llama_memory_clear(llama_get_memory(ctx), true)
        }
        sampler?.let { samplerPtr -> llama_sampler_reset(samplerPtr) }
    }

    private fun tokenize(text: String, addBos: Boolean): List<llama_token> {
        val utf8Count = text.encodeToByteArray().size
        val nTokens = utf8Count + (if (addBos) 1 else 0) + 1
        memScoped {
            val tokens = allocArray<IntVar>(nTokens)
            val tokenCount = llama_tokenize(vocab, text, utf8Count, tokens, nTokens, addBos, true)
            return List(tokenCount) { tokens[it] }
        }
    }

    private fun tokenToPiece(tokenId: Int): ByteArray {
        if (!isLoaded) error("Model not loaded. Call loadModel() first.")
        memScoped {
            val result = allocArray<ByteVar>(8) // Try with small buffer
            val nTokens = llama_token_to_piece(vocab, tokenId, result, 8, 0, false)
            return if (nTokens < 0) {
                // Buffer too small, allocate proper size
                val newResult = allocArray<ByteVar>(-nTokens) // Use absolute value as size
                val nNewTokens = llama_token_to_piece(vocab, tokenId, newResult, -nTokens, 0, false)
                newResult.readBytes(nNewTokens)
            } else {
                result.readBytes(nTokens)
            }
        }
    }

    override fun unloadModel() {
        if (isLoaded) {
            sampler?.let { llama_sampler_free(it) }
            context?.let { llama_free(it) }
            model?.let { llama_model_free(it) }

            model = null
            context = null
            vocab = null
            sampler = null
            isLoaded = false
        }
    }

    override val isModelLoaded: Boolean
        get() = isLoaded

    /**
     * Free the backend resources. Should be called on application termination.
     */
    override fun freeBackend() {
        LlamaNativeLogBridge.uninstall()
        llama_backend_free()
    }

    override fun getModelMetaValue(key: String): String? {
        val mdl = model ?: return null

        return memScoped {
            val bufferSize = 1024
            val buffer = allocArray<ByteVar>(bufferSize)

            val result = llama_model_meta_val_str(mdl, key, buffer, bufferSize.convert())

            if (result in 1..<bufferSize) {
                // Convert the null-terminated C string to Kotlin string
                buffer.readBytes(result).toKString()
            } else {
                null
            }
        }
    }

    override val supportsGpuOffloading: Boolean
        get() = llama_supports_gpu_offload()

    private fun selectPreferredGpuDevices(wantGpu: Boolean, platform: Platform): List<ggml_backend_dev_t?> {
        if (!wantGpu) return emptyList()
        if (!llama_supports_gpu_offload()) return emptyList()

        val preferred = preferredBackendNamesFor(platform)
        if (preferred.isEmpty()) {
            logger.info { "GPU not available for platform=$platform" }
            return emptyList()
        }

        val n = ggml_backend_dev_count().toInt()
        val picked = mutableListOf<ggml_backend_dev_t?>()
        logger.info { "Checking available devices..." }
        for (i in 0 until n) {
            val dev = ggml_backend_dev_get(i.toULong())?.also { logDevice(i, it) }
            if (ggml_backend_dev_type(dev) == ggml_backend_dev_type.GGML_BACKEND_DEVICE_TYPE_GPU) {
                val reg = ggml_backend_dev_backend_reg(dev)
                val name = ggml_backend_reg_name(reg)?.toKString() ?: ""
                if (preferred.any { it.equals(name, ignoreCase = true) }) {
                    picked += dev
                }
            }
        }

        return picked
    }

    private fun preferredBackendNamesFor(platform: Platform): List<String> =
        when (platform) {
            Platform.Android -> listOf("Vulkan", "OpenCL")
            Platform.IOS, Platform.MacOS -> listOf("Metal")
            Platform.Windows -> listOf("Vulkan")
            Platform.Linux -> listOf("CUDA", "Vulkan")
            else -> emptyList()
        }

    private fun logDevice(index: Int, dev: ggml_backend_dev_t) = memScoped {
        try {
            val reg = ggml_backend_dev_backend_reg(dev)
            val backend = ggml_backend_reg_name(reg)?.toKString() ?: "unknown"
            val name = ggml_backend_dev_name(dev)?.toKString() ?: "n/a"
            val desc = ggml_backend_dev_description(dev)?.toKString() ?: "n/a"
            val typeStr = when (ggml_backend_dev_type(dev)) {
                ggml_backend_dev_type.GGML_BACKEND_DEVICE_TYPE_CPU -> "CPU"
                ggml_backend_dev_type.GGML_BACKEND_DEVICE_TYPE_IGPU -> "iGPU"
                ggml_backend_dev_type.GGML_BACKEND_DEVICE_TYPE_GPU -> "GPU"
                ggml_backend_dev_type.GGML_BACKEND_DEVICE_TYPE_ACCEL -> "ACCEL"
                else -> "UNKNOWN"
            }

            val props = alloc<ggml_backend_dev_props>()
            ggml_backend_dev_get_props(dev, props.ptr)
            val free = props.memory_free.toLong()
            val total = props.memory_total.toLong()
            val caps = props.caps
            val buft = ggml_backend_dev_buffer_type(dev)
            val buftName = ggml_backend_buft_name(buft)?.toKString() ?: "n/a"

            logger.info {
                """
device[$index]: backend=$backend name=$name type=$typeStr buf=$buftName
desc=$desc mem=$free/$total
caps(async=${caps.async}, host=${caps.host_buffer}, fromHostPtr=${caps.buffer_from_host_ptr}, events=${caps.events})
""".trimIndent()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get backend device info" }
        }
    }
}

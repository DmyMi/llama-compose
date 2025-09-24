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
package cloud.dmytrominochkin.ai.llamacompose.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaModel
import cloud.dmytrominochkin.ai.llamacompose.llama.LlamaSamplingParams
import cloud.dmytrominochkin.ai.llamacompose.llama.logger
import cloud.dmytrominochkin.ai.llamacompose.proto.GenerationConfig
import cloud.dmytrominochkin.ai.llamacompose.proto.LlamaConfig
import cloud.dmytrominochkin.ai.llamacompose.proto.Theme
import cloud.dmytrominochkin.ai.llamacompose.settings.SettingsRepository
import cloud.dmytrominochkin.ai.llamacompose.theme.AppThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val isModelLoaded: Boolean = false,
    val isModelLoading: Boolean = false,
    val currentModelName: String = "",
    val showModelDownloadSheet: Boolean = false,
    val supportsGpu: Boolean = false,
    val modelLoadErrorMessage: String? = null,
)

class MainViewModel(
    private val llama: LlamaModel,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val logger = logger()

    val config = settingsRepository.configFlow

    private val _uiState = MutableStateFlow(MainState())
    val uiState = _uiState.asStateFlow()

    init {
        observeSettingsChanges()
        viewModelScope.launch {
            val gpuSupport = runCatching { llama.supportsGpuOffloading }
            runCatching { _uiState.update { it.copy(supportsGpu = gpuSupport.getOrNull() ?: false) } } }
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsRepository.configFlow.collect { config ->
                if (_uiState.value.isModelLoaded) {
                    updateModelSamplingParams(config.generation!!)
                }
            }
        }
    }

    private suspend fun updateModelSamplingParams(config: GenerationConfig) {
        val samplingParams = config.toLlamaSamplingParams()
        val success = llama.updateSamplingParams(samplingParams)
        if (success) {
            logger.debug { "Successfully updated sampling parameters" }
        } else {
            logger.warn { "Failed to update sampling parameters" }
        }
    }

    fun loadModel(modelPath: String, modelName: String) = viewModelScope.launch {
        _uiState.update { it.copy(isModelLoading = true, modelLoadErrorMessage = null) }
        try {
            // Get current settings for model loading
            val currentConfig = settingsRepository.configFlow.first()
            val samplingParams = currentConfig.generation

            // Apply model params for next load
            val newParams = llama.getModelParams().copy(
                nCtx = currentConfig.model!!.nctx,
                useGpu = currentConfig.model.use_gpu
            )
            llama.setModelParams(newParams)
            val success = llama.loadModel(modelPath, samplingParams!!.toLlamaSamplingParams())
            _uiState.update { it.copy(isModelLoaded = success) }
            if (success) {
                _uiState.update { it.copy(showModelDownloadSheet = false, currentModelName = modelName, modelLoadErrorMessage = null) }
            } else {
                _uiState.update { it.copy(modelLoadErrorMessage = "Failed to load $modelName") }
            }
        } finally {
            _uiState.update { it.copy(isModelLoading = false) }
        }
    }

    fun unloadModel() = viewModelScope.launch {
        _uiState.update { it.copy(isModelLoading = true) }
        try {
            llama.unloadModel()
            _uiState.update { it.copy(isModelLoaded = false) }
            _uiState.update { it.copy(currentModelName = "") }
        } finally {
            _uiState.update { it.copy(isModelLoading = false) }
        }
    }

    fun toggleModelDownloadSheet() {
        _uiState.update { it.copy(showModelDownloadSheet = !it.showModelDownloadSheet) }
    }

    fun hideModelDownloadSheet() {
        _uiState.update { it.copy(showModelDownloadSheet = false) }
    }

    fun clearModelLoadError() {
        _uiState.update { it.copy(modelLoadErrorMessage = null) }
    }

    suspend fun updateConfig(transform: (LlamaConfig) -> LlamaConfig) = settingsRepository.update(transform)

    fun saveOnBoardingState(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(is_on_boarding_completed = completed) }
        }
    }

    fun setTheme(theme: Theme) {
        // Update persisted settings and in-memory override for immediate effect
        viewModelScope.launch {
            settingsRepository.update { it.copy(theme = theme) }
        }
        AppThemeConfig.userOverride.value = theme
    }

    override fun onCleared() {
        super.onCleared()
        llama.freeBackend()
    }
}

// Extension function to convert GenerationConfig to LlamaSamplingParams
private fun GenerationConfig.toLlamaSamplingParams(): LlamaSamplingParams {
    return LlamaSamplingParams(
        temperature = temperature, // Greedy overrides temperature
        topK = top_k,
        topP = top_p,
        minP = min_p,
        repeatPenalty = repeat_penalty,
        seed = seed,
        greedy = greedy
    )
}

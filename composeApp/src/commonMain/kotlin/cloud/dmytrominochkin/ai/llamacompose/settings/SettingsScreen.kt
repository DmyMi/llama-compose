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
package cloud.dmytrominochkin.ai.llamacompose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.dmytrominochkin.ai.llamacompose.components.AnimatedTabHeader
import cloud.dmytrominochkin.ai.llamacompose.components.VerticalScrollbar
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownPadding
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownTypography
import cloud.dmytrominochkin.ai.llamacompose.main.MainViewModel
import cloud.dmytrominochkin.ai.llamacompose.proto.GenerationConfig
import cloud.dmytrominochkin.ai.llamacompose.proto.ModelConfig
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_context_length
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_context_length_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_gpu_cancel
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_gpu_confirm_message
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_gpu_confirm_title
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_gpu_enable
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_gpu_unavailable
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_greedy
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_greedy_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_inference_tab
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_inference_title
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_min_p
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_min_p_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_model_subtitle
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_model_tab
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_model_title
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_repeat_penalty
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_repeat_penalty_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_save
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_temperature
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_temperature_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_top_k
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_top_k_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_top_p
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_top_p_desc
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_use_gpu
import cloud.dmytrominochkin.ai.llamacompose.resources.settings_use_gpu_desc
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(mainViewModel: MainViewModel, onDone: () -> Unit) {
    val config by mainViewModel.config.collectAsState(getDefaultLlamaConfig())
    val uiState by mainViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var workingConfig by remember(config) { mutableStateOf(config) }

    Column(modifier = Modifier.fillMaxSize().padding(AppDimension.spacingM)) {
        AnimatedTabHeader(
            tabs = listOf(
                stringResource(Res.string.settings_model_tab),
                stringResource(Res.string.settings_inference_tab)
            ),
            selectedIndex = selectedTabIndex,
            onSelected = { selectedTabIndex = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(AppDimension.spacingM))

        when (selectedTabIndex) {
            0 -> ModelSettingsTab(
                model = workingConfig.model!!,
                isGpuAvailable = { uiState.supportsGpu },
                onModelChanged = { workingConfig = workingConfig.copy(model = it) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            else -> InferenceSettingsTab(
                generation = workingConfig.generation!!,
                onGenerationChanged = { workingConfig = workingConfig.copy(generation = it) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = {
                scope.launch {
                    mainViewModel.updateConfig { workingConfig }
                    onDone()
                }
            }) { Text(stringResource(Res.string.settings_save)) }
        }
    }
}

@Composable
private fun ModelSettingsTab(
    model: ModelConfig,
    isGpuAvailable: () -> Boolean,
    onModelChanged: (ModelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showGpuConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            tonalElevation = AppDimension.elevationS,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(AppDimension.elevationS),
            shape = MaterialTheme.shapes.medium
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(AppDimension.spacingS),
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingS)
            ) {
                item {
                    Text(stringResource(Res.string.settings_model_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(Res.string.settings_model_subtitle), style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text(stringResource(Res.string.settings_context_length), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.settings_context_length_desc), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingM)) {
                        listOf(2048, 4096).forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = model.nctx == option, onClick = {
                                    onModelChanged(model.copy(nctx = option))
                                })
                                Spacer(Modifier.width(AppDimension.spacingXS))
                                Text(option.toString())
                            }
                        }
                    }
                }
                val gpuAvailable = isGpuAvailable()
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.settings_use_gpu), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.settings_use_gpu_desc), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = model.use_gpu,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showGpuConfirm = true
                                } else {
                                    onModelChanged(model.copy(use_gpu = false))
                                }
                            },
                            // If it was enabled -> allow to disable it
                            enabled = gpuAvailable || model.use_gpu
                        )
                    }
                }
                item {
                    if (!gpuAvailable) {
                        Text(stringResource(Res.string.settings_gpu_unavailable), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        VerticalScrollbar(scrollState = listState)
    }

    if (showGpuConfirm) {
        AlertDialog(
            onDismissRequest = { showGpuConfirm = false },
            title = { Text(stringResource(Res.string.settings_gpu_confirm_title)) },
            text = {
                Markdown(
                    content = stringResource(Res.string.settings_gpu_confirm_message),
                    typography = appMarkdownTypography(),
                    padding = appMarkdownPadding(),
                    modifier = Modifier.wrapContentSize()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onModelChanged(model.copy(use_gpu = true))
                    showGpuConfirm = false
                }) { Text(stringResource(Res.string.settings_gpu_enable)) }
            },
            dismissButton = {
                TextButton(onClick = { showGpuConfirm = false }) { Text(stringResource(Res.string.settings_gpu_cancel)) }
            }
        )
    }
}

@Composable
private fun InferenceSettingsTab(
    generation: GenerationConfig,
    onGenerationChanged: (GenerationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            tonalElevation = AppDimension.elevationS,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(AppDimension.elevationS),
            shape = MaterialTheme.shapes.medium
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(AppDimension.spacingS),
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingS)
            ) {
                item {
                    Text(stringResource(Res.string.settings_inference_title), style = MaterialTheme.typography.titleLarge)
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.settings_greedy), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.settings_greedy_desc), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = generation.greedy,
                            onCheckedChange = { checked -> onGenerationChanged(generation.copy(greedy = checked)) }
                        )
                    }
                }
                item {
                    Text(stringResource(Res.string.settings_temperature, generation.temperature))
                    Text(stringResource(Res.string.settings_temperature_desc), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = generation.temperature,
                        onValueChange = { v -> onGenerationChanged(generation.copy(temperature = v.roundToDecimals())) },
                        valueRange = 0f..2f,
                        enabled = !generation.greedy,
                        steps = 20
                    )
                }
                item {
                    Text(stringResource(Res.string.settings_top_k, generation.top_k))
                    Text(stringResource(Res.string.settings_top_k_desc), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = generation.top_k.toFloat(),
                        onValueChange = { v -> onGenerationChanged(generation.copy(top_k = v.toInt())) },
                        valueRange = 0f..200f,
                        enabled = !generation.greedy,
                        steps = 100
                    )
                }
                item {
                    Text(stringResource(Res.string.settings_top_p, generation.top_p))
                    Text(stringResource(Res.string.settings_top_p_desc), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = generation.top_p,
                        onValueChange = { v -> onGenerationChanged(generation.copy(top_p = v.roundToDecimals(2))) },
                        valueRange = 0f..1f,
                        enabled = !generation.greedy,
                        steps = 20
                    )
                }
                item {
                    Text(stringResource(Res.string.settings_min_p, generation.min_p))
                    Text(stringResource(Res.string.settings_min_p_desc), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = generation.min_p,
                        onValueChange = { v -> onGenerationChanged(generation.copy(min_p = v.roundToDecimals(2))) },
                        valueRange = 0f..1f,
                        enabled = !generation.greedy,
                        steps = 20
                    )
                }
                item {
                    Text(stringResource(Res.string.settings_repeat_penalty, generation.repeat_penalty))
                    Text(stringResource(Res.string.settings_repeat_penalty_desc), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = generation.repeat_penalty,
                        onValueChange = { v -> onGenerationChanged(generation.copy(repeat_penalty = v.roundToDecimals())) },
                        valueRange = 0.5f..2.0f,
                        // TODO: Implement penalties UI
                        enabled = false,
                        steps = 10
                    )
                }
            }
        }

        VerticalScrollbar(scrollState = listState)
    }
}



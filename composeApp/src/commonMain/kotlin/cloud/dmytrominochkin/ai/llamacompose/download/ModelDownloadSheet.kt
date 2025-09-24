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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.dmytrominochkin.ai.llamacompose.components.KottieAnimationCard
import cloud.dmytrominochkin.ai.llamacompose.main.MainViewModel
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.model_initializing
import cloud.dmytrominochkin.ai.llamacompose.resources.model_selection
import cloud.dmytrominochkin.ai.llamacompose.resources.model_unload_current
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadSheet(
    mainViewModel: MainViewModel,
    modelDownloadViewModel: ModelDownloadViewModel,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val uiState by mainViewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimension.spacingM)
        ) {
            Text(
                text = stringResource(Res.string.model_selection),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = AppDimension.spacingM)
            )

            if (uiState.isModelLoaded && !uiState.isModelLoading) {
                OutlinedButton(
                    onClick = {
                        mainViewModel.unloadModel()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimension.spacingM)
                ) {
                    Text(stringResource(Res.string.model_unload_current, uiState.currentModelName.ifBlank { "AI" }))
                }
            }

            if (uiState.isModelLoading) {
                Box (
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimension.spacingXL)
                ) {
                    KottieAnimationCard(
                        animationFile = "processing.json",
                        text = stringResource(Res.string.model_initializing),
                        animationSize = AppDimension.animationSizeM,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )
                }
            } else {
                Box(modifier = Modifier.height(500.dp)) {
                    ModelDownloadScreen(
                        viewModel = modelDownloadViewModel,
                        snackbarHostState = snackbarHostState,
                        onLoadClicked = { modelPath, modelName ->
                            if (!uiState.isModelLoaded) {
                                mainViewModel.loadModel(modelPath, modelName)
                            }
                        },
                        loadedModelName = uiState.currentModelName,
                        isModelLoaded = uiState.isModelLoaded
                    )
                }
            }
        }
    }
}

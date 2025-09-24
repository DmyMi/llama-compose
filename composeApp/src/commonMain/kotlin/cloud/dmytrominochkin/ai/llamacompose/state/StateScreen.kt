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
package cloud.dmytrominochkin.ai.llamacompose.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cloud.dmytrominochkin.ai.llamacompose.components.VerticalScrollbar
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownPadding
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownTypography
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.back_to_agent
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StateScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StateViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState(emptyList())
    val listState = rememberLazyListState()
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(AppDimension.spacingS),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(start = AppDimension.spacingS, end = AppDimension.spacingS, bottom = AppDimension.spacingS)
                ) {
                    items(state) {
                        Column(
                            modifier = Modifier.padding(AppDimension.spacingS),
                        ) {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = AppDimension.spacingXS)
                            )
                            Markdown(
                                content = it.content,
                                typography = appMarkdownTypography(),
                                padding = appMarkdownPadding()
                            )
                        }
                    }
                }

                VerticalScrollbar(scrollState = listState)
            }
            OutlinedButton(
                onClick = { onDone() }
            ) {
                Text(stringResource(Res.string.back_to_agent))
            }
        }
    }
}

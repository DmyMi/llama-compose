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
package cloud.dmytrominochkin.ai.llamacompose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.cd_select_model
import cloud.dmytrominochkin.ai.llamacompose.resources.model_select_hint
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import cloud.dmytrominochkin.ai.llamacompose.theme.LlamaComposeTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ModelSelectHint(
    onSelectModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(AppDimension.spacingL)
        ) {
            val downloadIconId = "downloadIcon"
            val settingsIconId = "settingsIcon"
            val textWithIcon = buildAnnotatedString {
                val hintText = stringResource(Res.string.model_select_hint)
                val parts = hintText.split("[icon1]")
                
                append(parts[0])
                appendInlineContent(downloadIconId, "[icon1]")
                if (parts.size > 1) {
                    val remainingParts = parts[1].split("[icon2]")
                    append(remainingParts[0])
                    appendInlineContent(settingsIconId, "[icon2]")
                    if (remainingParts.size > 1) {
                        append(remainingParts[1])
                    }
                }
            }
            
            val inlineContent = mapOf(
                downloadIconId to InlineTextContent(
                    placeholder = Placeholder(
                        width = 20.sp,
                        height = 20.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                settingsIconId to InlineTextContent(
                    placeholder = Placeholder(
                        width = 20.sp,
                        height = 20.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
            
            Text(
                text = textWithIcon,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.padding(AppDimension.spacingS))
            
            Button(
                onClick = onSelectModelClick,
                modifier = Modifier.padding(top = AppDimension.spacingXS)
            ) {
                Text(stringResource(Res.string.cd_select_model))
            }
        }
    }
}

@Preview(locale = "en")
@Preview(locale = "es")
@Preview(locale = "uk")
@Composable
fun ModelSelectHintPreview() {
    LlamaComposeTheme {
        Surface {
            ModelSelectHint({})
        }
    }
}

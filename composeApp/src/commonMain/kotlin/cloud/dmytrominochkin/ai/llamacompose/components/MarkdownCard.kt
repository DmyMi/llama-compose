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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import cloud.dmytrominochkin.ai.llamacompose.theme.LlamaComposeTheme
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MarkdownCard(
    title: String,
    markdown: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(AppDimension.spacingS),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Markdown(
            content = markdown,
            typography = appMarkdownTypography(text = MaterialTheme.typography.labelSmall),
            padding = appMarkdownPadding(),
            modifier = Modifier.padding(horizontal = AppDimension.spacingS, vertical = 0.dp)
        )
        Spacer(modifier = Modifier.height(AppDimension.spacingS))
    }
}

@Preview
@Composable
fun MarkdownCardPreview() {
    LlamaComposeTheme {
        MarkdownCard(
            title = "Helpful title",
            markdown = "Some `markdown` content as **string**"
        )
    }
}

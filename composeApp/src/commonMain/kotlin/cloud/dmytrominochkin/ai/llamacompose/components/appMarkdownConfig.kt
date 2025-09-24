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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownPadding
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.markdownPadding

@Composable
fun appMarkdownTypography(
    h1: TextStyle = MaterialTheme.typography.displayMedium,
    h2: TextStyle = MaterialTheme.typography.displaySmall,
    h3: TextStyle = MaterialTheme.typography.headlineMedium,
    h4: TextStyle = MaterialTheme.typography.headlineSmall,
    h5: TextStyle = MaterialTheme.typography.titleMedium,
    h6: TextStyle = MaterialTheme.typography.titleSmall,
    text: TextStyle = MaterialTheme.typography.bodyMedium,
    code: TextStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    inlineCode: TextStyle = text.copy(fontFamily = FontFamily.Monospace),
    quote: TextStyle = MaterialTheme.typography.bodyMedium.plus(SpanStyle(fontStyle = FontStyle.Italic)),
    paragraph: TextStyle = MaterialTheme.typography.bodyMedium,
    ordered: TextStyle = MaterialTheme.typography.bodyMedium,
    bullet: TextStyle = MaterialTheme.typography.bodyMedium,
    list: TextStyle = MaterialTheme.typography.bodyMedium,
    link: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline
    ),
    textLink: TextLinkStyles = TextLinkStyles(style = link.toSpanStyle()),
    table: TextStyle = text,
): MarkdownTypography = DefaultMarkdownTypography(
    h1 = h1,
    h2 = h2,
    h3 = h3,
    h4 = h4,
    h5 = h5,
    h6 = h6,
    text = text,
    quote = quote,
    code = code,
    inlineCode = inlineCode,
    paragraph = paragraph,
    ordered = ordered,
    bullet = bullet,
    list = list,
    textLink = textLink,
    table = table,
)

@Composable
fun appMarkdownPadding(
    block: Dp = 1.dp,
    list: Dp = 2.dp,
    listItemTop: Dp = 2.dp,
    listItemBottom: Dp = 2.dp,
    listIndent: Dp = 4.dp,
    codeBlock: PaddingValues = PaddingValues(4.dp),
    blockQuote: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    blockQuoteText: PaddingValues = PaddingValues(vertical = 2.dp),
    blockQuoteBar: PaddingValues.Absolute = PaddingValues.Absolute(left = 2.dp, top = 1.dp, right = 2.dp, bottom = 1.dp),
): MarkdownPadding = markdownPadding(
    block = block,
    list = list,
    listItemBottom = listItemBottom,
    listItemTop = listItemTop,
    listIndent = listIndent,
    codeBlock = codeBlock,
    blockQuote = blockQuote,
    blockQuoteText = blockQuoteText,
    blockQuoteBar = blockQuoteBar,
)

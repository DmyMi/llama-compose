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
package cloud.dmytrominochkin.ai.llamacompose.onboard

import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownPadding
import cloud.dmytrominochkin.ai.llamacompose.components.appMarkdownTypography
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue

@Composable
fun OnBoardItem(
    pageNumber: Int,
    page: OnBoardModel,
    pagerState: PagerState
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(page.image),
            contentDescription = null,
            modifier = Modifier
                .pagerIconTransition(pageNumber, pagerState)
                .height(350.dp)
                .width(350.dp)
                .padding(bottom = AppDimension.spacingL)
        )
        if (page.titleAnnotatedString != null) {
            Text(
                text = page.titleAnnotatedString,
                inlineContent = page.titleInlineContent ?: emptyMap(),
                modifier = Modifier.pagerTextTransition(pageNumber, pagerState),
                style = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
            )
        } else {
            Text(
                text = page.title,
                modifier = Modifier.pagerTextTransition(pageNumber, pagerState),
                style = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
            )
        }
        Markdown(
            content = page.description,
            modifier = Modifier.pagerTextTransition(pageNumber, pagerState)
                .padding(horizontal = AppDimension.spacingM, vertical = AppDimension.spacingS),
            typography = appMarkdownTypography(),
            padding = appMarkdownPadding(),
            components = markdownComponents(
                paragraph = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MarkdownParagraph(
                            it.content,
                            it.node,
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            )
        )

    }
}

fun Modifier.pagerTextTransition(page: Int, pagerState: PagerState) = graphicsLayer {
    val pageOffset = pagerState.getOffsetDistanceInPages(page)
    val scale = lerp(0.7f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))

    scaleX = scale
    scaleY = scale
    alpha = lerp(0.5f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))

    translationX = EaseOutQuad.transform(pageOffset * size.width / 2f)
}

fun Modifier.pagerIconTransition(page: Int, pagerState: PagerState) = graphicsLayer {
    val pageOffset = pagerState.getOffsetDistanceInPages(page)

    if (pageOffset <= 0f) {
        rotationY = 42f * pageOffset.absoluteValue
        rotationZ = -38f * pageOffset.absoluteValue
    } else if (pageOffset <= 1f) {
        rotationY = -42f * pageOffset.absoluteValue
        rotationZ = 38f * pageOffset.absoluteValue
    } else if (pageOffset >= 1f) {
        rotationY = -42f * pageOffset.absoluteValue
        rotationZ = 38f * pageOffset.absoluteValue
    }

    alpha = lerp(0.5f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
}

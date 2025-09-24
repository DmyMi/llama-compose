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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.dmytrominochkin.ai.llamacompose.llama.Platform
import cloud.dmytrominochkin.ai.llamacompose.llama.currentPlatform
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard01
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard02
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard03
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard04
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard05_android
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard05_apple
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard05_vulkan
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_agent_description
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_agent_title
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_back
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_chat_description
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_chat_title
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_models_description
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_models_title
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_next
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_platform_description
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_platform_title
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_skip
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_start
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_welcome_description
import cloud.dmytrominochkin.ai.llamacompose.resources.onboard_welcome_title
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun getPlatformNameString(): String {
    return when (currentPlatform) {
        Platform.Android -> "Android"
        Platform.Windows, Platform.Linux, Platform.Other -> "Vulkan"
        Platform.MacOS, Platform.IOS, Platform.Simuator -> "Apple Silicon"
    }
}

@Composable
private fun buildTitleWithTrailingIcon(stringRes: StringResource, icon: ImageVector): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val iconId = "inlineIcon"
    val titleText = stringResource(stringRes)
    
    val annotatedString = buildAnnotatedString {
        append("$titleText ")
        appendInlineContent(iconId, "[icon]")
    }
    
    val inlineContent = mapOf(
        iconId to InlineTextContent(
            placeholder = Placeholder(
                width = 24.sp,
                height = 24.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
    
    return Pair(annotatedString, inlineContent)
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onOnboardingComplete: () -> Unit = {}
) {
    val platformName = getPlatformNameString()
    val (chatTitleAnnotated, chatTitleInline) = buildTitleWithTrailingIcon(Res.string.onboard_chat_title, Icons.AutoMirrored.Filled.Chat)
    val (agentTitleAnnotated, agentTitleInline) = buildTitleWithTrailingIcon(Res.string.onboard_agent_title, Icons.Default.SmartToy)
    
    val pages = listOf(
        OnBoardModel(
            title = stringResource(Res.string.onboard_welcome_title),
            description = stringResource(Res.string.onboard_welcome_description),
            image = Res.drawable.onboard01,
        ),
        OnBoardModel(
            title = stringResource(Res.string.onboard_models_title),
            description = stringResource(Res.string.onboard_models_description),
            image = Res.drawable.onboard02
        ),
        OnBoardModel(
            title = stringResource(Res.string.onboard_chat_title),
            description = stringResource(Res.string.onboard_chat_description),
            image = Res.drawable.onboard03,
            titleAnnotatedString = chatTitleAnnotated,
            titleInlineContent = chatTitleInline
        ),
        OnBoardModel(
            title = stringResource(Res.string.onboard_agent_title),
            description = stringResource(Res.string.onboard_agent_description),
            image = Res.drawable.onboard04,
            titleAnnotatedString = agentTitleAnnotated,
            titleInlineContent = agentTitleInline
        ),
        OnBoardModel(
            title = stringResource(Res.string.onboard_platform_title),
            description = stringResource(Res.string.onboard_platform_description, platformName),
            image = when (currentPlatform) {
                Platform.MacOS, Platform.IOS, Platform.Simuator -> Res.drawable.onboard05_apple
                Platform.Android -> Res.drawable.onboard05_android
                Platform.Windows, Platform.Linux, Platform.Other -> Res.drawable.onboard05_vulkan
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Surface {
        Column(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(AppDimension.spacingM)
        ) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondViewportPageCount = 1
            ) { page ->
                OnBoardItem(
                    page,
                    pages[page],
                    pagerState
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimension.spacingS)

            ) {

                TextButton(onClick = {
                    if (pagerState.currentPage > 0) {
                        // Back button behavior
                        val previousPage = pagerState.currentPage - 1
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                previousPage,
                                animationSpec = tween(durationMillis = 700, easing = EaseOutQuad)
                            )
                        }
                    } else {
                        // Skip button behavior
                        val skipPage = pagerState.pageCount - 1
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                skipPage,
                                animationSpec = tween(durationMillis = 700, easing = EaseOutQuad)
                            )
                        }
                    }
                }) {
                    Text(
                        if (pagerState.currentPage > 0)
                            stringResource(Res.string.onboard_back)
                        else
                            stringResource(Res.string.onboard_skip),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(AppDimension.spacingXS)
                                .width(if (isSelected) AppDimension.spacingM else AppDimension.spacingS)
                                .height(AppDimension.spacingS)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(AppDimension.radiusL)
                                )
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                    shape = CircleShape
                                )
                        )
                    }
                }


                TextButton(
                    onClick = {
                        if (pagerState.currentPage < pagerState.pageCount - 1) {
                            val nextPage = pagerState.currentPage + 1
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    nextPage,
                                    animationSpec = tween(durationMillis = 700, easing = EaseOutQuad)
                                )
                            }
                        } else {
                            // User clicked "Start" on the last page
                            onOnboardingComplete()
                        }
                    },
                ) {
                    Text(
                        if (pagerState.currentPage < pagerState.pageCount - 1)
                            stringResource(Res.string.onboard_next)
                        else
                            stringResource(Res.string.onboard_start),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun OnboardingScreenPreview() {
    OnboardingScreen()
}

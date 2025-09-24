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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import cloud.dmytrominochkin.ai.llamacompose.theme.LlamaComposeTheme
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
internal fun AnimatedTabHeader(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = { positions ->
            TabIndicatorContainer(tabPositions = positions, selectedIndex = selectedIndex) {
                val color = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(AppDimension.spacingXS)) {
                    drawCircle(color)
                }
            }
        },
        modifier = modifier
    ) {
        tabs.forEachIndexed { index, tab ->
            val color = animateColorAsState(
                if (index == selectedIndex) MaterialTheme.colorScheme.primary else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), label = "color_animation"
            )
            Tab(
                selected = index == selectedIndex,
                text = { Text(text = tab, color = color.value) },
                onClick = {
                    onSelected(index)
                },
                selectedContentColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun TabIndicatorContainer(
    tabPositions: List<TabPosition>,
    selectedIndex: Int,
    content: @Composable() () -> Unit
) {
    val transition = updateTransition(targetState = selectedIndex, label = "selectedIndex")

    val offset = transition.animateDp(label = "tabPositions",
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        }) { idx ->
        val position = tabPositions[idx]
        (position.left + position.right) / 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomStart)
            .offset { IntOffset(offset.value.roundToPx(), (-2).dp.roundToPx()) }
    ) {
        content()
    }
}

@Preview
@Composable
fun AnimatedTabHeaderPreview() {
    LlamaComposeTheme {
        AnimatedTabHeader(
            tabs = listOf("Very", "Good", "Tab", "Header"),
            selectedIndex = 1,
            onSelected = {}
        )
    }
}

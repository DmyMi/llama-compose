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

import KottieAnimation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import utils.KottieConstants


/**
 * A reusable card component that displays a Kottie animation with consistent light background and optional text.
 *
 * @param animationFile The animation file name (without path) from the resources/files directory
 * @param text Optional text to display below the animation
 * @param modifier Modifier to be applied to the card
 * @param animationSize Size of the animation (default: 120.dp)
 */
@Composable
fun KottieAnimationCard(
    animationFile: String,
    text: String? = null,
    modifier: Modifier = Modifier,
    animationSize: Dp = AppDimension.animationSizeM
) {
    var animation by remember { mutableStateOf("") }

    LaunchedEffect(animationFile) {
        animation = Res.readBytes("files/$animationFile").decodeToString()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White // Always white background regardless of theme
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.then(
                Modifier
                    .padding(AppDimension.spacingL)
            )
        ) {
            val composition = rememberKottieComposition(
                spec = KottieCompositionSpec.File(animation)
            )

            val animationState by animateKottieCompositionAsState(
                composition = composition,
                iterations = KottieConstants.IterateForever
            )

            KottieAnimation(
                composition = composition,
                progress = { animationState.progress },
                modifier = Modifier.requiredSize(animationSize),
                backgroundColor = Color.White
            )

            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Black, // Black text for contrast with light background
                    modifier = Modifier.padding(top = AppDimension.spacingM)
                )
            }
        }
    }
}

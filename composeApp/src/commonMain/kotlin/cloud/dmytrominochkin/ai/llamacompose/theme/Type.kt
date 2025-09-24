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
package cloud.dmytrominochkin.ai.llamacompose.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.noto_sans
import cloud.dmytrominochkin.ai.llamacompose.resources.oswald
import org.jetbrains.compose.resources.Font

@Composable
fun bodyFontFamily() = FontFamily(
    Font(
        resource = Res.font.noto_sans,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    )
)
@Composable
fun displayFontFamily() = FontFamily(
    Font(
        resource = Res.font.oswald,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    )
)

// Default Material 3 typography values
val baseline = Typography()

@Composable
fun appTypography() = Typography().run {
    val bodyFont = bodyFontFamily()
    val displayFont = displayFontFamily()
    copy(
        displayLarge = baseline.displayLarge.copy(fontFamily = displayFont),
        displayMedium = baseline.displayMedium.copy(fontFamily = displayFont),
        displaySmall = baseline.displaySmall.copy(fontFamily = displayFont),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFont),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFont),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFont),
        titleLarge = baseline.titleLarge.copy(fontFamily = displayFont),
        titleMedium = baseline.titleMedium.copy(fontFamily = displayFont),
        titleSmall = baseline.titleSmall.copy(fontFamily = displayFont),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFont),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFont),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFont),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFont),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFont),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFont),
    )
}


/**
* Found this on the internet years ago, can't really provide attribution
*/
package cloud.dmytrominochkin.ai.llamacompose.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.dmytrominochkin.ai.llamacompose.theme.AppDimension
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


private const val ASPECT_RATIO = 2f
private const val CIRCLE_ANGLE = 360f
private const val MIN_ANGLE = 0f
private const val MAX_ANGLE = 180f
const val DEFAULT_DURATION = 300
const val DEFAULT_DELAY = 100

val THICKNESS = 15.dp
val SCALE_PADDING = 24.dp
val SCALE_STROKE_WIDTH = 2.dp
val SCALE_STROKE_LENGTH = 16.dp
const val START_ANGLE = -180f
val JOIN_STYLE = DialJoinStyle.WithDegreeGap(2f)

val MainLabel: @Composable (value: Any) -> Unit = {
    Text(text = it.toString(), fontSize = 24.sp)
}

val MinAndMaxValueLabel: @Composable (value: Any) -> Unit = {
    Text(
        text = it.toString(),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(top = AppDimension.spacingM)
    )
}

data class DialConfig(
    val thickness: Dp = THICKNESS,
    val scalePadding: Dp = SCALE_PADDING,
    val scaleLineWidth: Dp = SCALE_STROKE_WIDTH,
    val scaleLineLength: Dp = SCALE_STROKE_LENGTH,
    val joinStyle: DialJoinStyle = JOIN_STYLE,
    val displayScale: Boolean = true,
    val roundCorners: Boolean = false,
)

sealed class DialJoinStyle {
    data object Joined : DialJoinStyle()
    data object Overlapped : DialJoinStyle()
    data class WithDegreeGap(val degrees: Float) : DialJoinStyle()
}

fun Float.mapValueToDifferentRange(
    inMin: Float,
    inMax: Float,
    outMin: Float,
    outMax: Float,
) = (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin

@Composable
fun Gauge(
    value: Int,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier,
    animation: (() -> AnimationSpec<Float>)? = {
        tween(DEFAULT_DURATION, DEFAULT_DELAY)
    },
    config: DialConfig = DialConfig(),
    minAndMaxValueLabel: @Composable (value: Int) -> Unit = MinAndMaxValueLabel,
    mainLabel: @Composable (value: Int) -> Unit = MainLabel,
) {
    var animationPlayed by remember {
        mutableStateOf(animation == null)
    }
    LaunchedEffect(value) {
        animationPlayed = true
    }

    val animatedScale = if (animation == null) {
        1f
    } else {
        animateFloatAsState(
            targetValue = if (animationPlayed) 1f else 0f,
            animationSpec = animation()
        ).value
    }

    val targetProgress = value.coerceIn(minValue..maxValue) * animatedScale

    Box(modifier = modifier) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            val primary = when (value) {
                in 0..20 -> Color(0xFF81C784) // Light green
                in 21..40 -> Color(0xFF4CAF50) // Darker green
                in 41..60 -> Color(0xFFFFEB3B) // Yellow
                in 61..80 -> Color(0xFFFF9800) // Orange
                else -> Color(0xFFF44336) // Red
            }
            val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ASPECT_RATIO)
                    .drawBehind {
                        drawProgressBar(
                            value = targetProgress,
                            minValue = minValue.toFloat(),
                            maxValue = maxValue.toFloat(),
                            config = config,
                            progressBarColor = primary,
                            progressBarBackgroundColor = grid,
                        )

                        if (config.displayScale) {
                            drawScale(
                                color = grid,
                                center = Offset(
                                    center.x,
                                    size.height - (config.scaleLineWidth.toPx() / 2f)
                                ),
                                config = config,
                            )
                        }
                    }
            ) {
                val desiredHeight = maxWidth / ASPECT_RATIO
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = desiredHeight / 2f)
                ) {
                    mainLabel(value)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                minAndMaxValueLabel(minValue)
                minAndMaxValueLabel(maxValue)
            }
        }
    }
}

private fun DrawScope.drawProgressBar(
    value: Float,
    minValue: Float,
    maxValue: Float,
    config: DialConfig,
    progressBarColor: Color,
    progressBarBackgroundColor: Color,
) {
    val sweepAngle = value.mapValueToDifferentRange(minValue, maxValue, MIN_ANGLE, MAX_ANGLE)
    val thickness = config.thickness.toPx()
    val radius = (size.width - thickness) / 2f
    val circumference = (2f * PI * radius).toFloat()
    val thicknessInDegrees = CIRCLE_ANGLE * thickness / circumference
    val arcPadding = if (config.roundCorners) thicknessInDegrees / 2f else 0f
    val topLeftOffset = Offset(thickness / 2f, thickness / 2f)
    // Arc has to be drawn on 2 * height space cause we want only half of the circle
    val arcSize = Size(size.width - thickness, size.height * ASPECT_RATIO - thickness)
    val style = Stroke(
        width = thickness,
        cap = config.strokeCap,
        pathEffect = PathEffect.cornerPathEffect(20f)
    )

    val joinStyle = if (value == minValue || value == maxValue)
        DialJoinStyle.Joined
    else
        config.joinStyle

    if (value > minValue) {
        drawArc(
            color = progressBarColor,
            startAngle = START_ANGLE + arcPadding,
            sweepAngle = sweepAngle - (2f * arcPadding),
            useCenter = false,
            style = style,
            topLeft = topLeftOffset,
            size = arcSize
        )
    }

    if (value < maxValue) {
        drawArc(
            color = progressBarBackgroundColor,
            startAngle = START_ANGLE + sweepAngle + joinStyle.startAnglePadding(arcPadding),
            sweepAngle = (MAX_ANGLE - sweepAngle - joinStyle.sweepAnglePadding(arcPadding))
                .coerceAtLeast(0f),
            useCenter = false,
            style = style,
            topLeft = topLeftOffset,
            size = arcSize
        )
    }
}

private const val MAX_LINE_LENGTH = 0.20f
private const val MINOR_SCALE_ALPHA = 0.5f
private const val MINOR_SCALE_LENGTH_FACTOR = 0.35f
private const val SCALE_STEP = 2
private const val MAJOR_SCALE_MODULO = 5 * SCALE_STEP
private fun DrawScope.drawScale(
    color: Color,
    center: Offset,
    config: DialConfig,
) {
    val scaleLineLength = (config.scaleLineLength.toPx() / center.x).coerceAtMost(MAX_LINE_LENGTH)
    val scalePadding = (config.thickness.toPx() + config.scalePadding.toPx()) / center.x
    val startRadiusFactor = 1 - scalePadding - scaleLineLength
    val endRadiusFactor = startRadiusFactor + scaleLineLength
    val smallLineRadiusFactor = scaleLineLength * MINOR_SCALE_LENGTH_FACTOR
    val scaleMultiplier = size.width / 2f

    for (point in 0..100 step SCALE_STEP) {
        val angle = (
                point.toFloat()
                    .mapValueToDifferentRange(
                        0f,
                        100f,
                        START_ANGLE,
                        0f
                    )
                ) * PI.toFloat() / 180f // to radians
        val startPos = point.position(
            angle,
            scaleMultiplier,
            startRadiusFactor,
            smallLineRadiusFactor
        )
        val endPos = point.position(
            angle,
            scaleMultiplier,
            endRadiusFactor,
            -smallLineRadiusFactor
        )
        drawLine(
            color = if (point % MAJOR_SCALE_MODULO == 0)
                color
            else
                color.copy(alpha = MINOR_SCALE_ALPHA),
            start = center + startPos,
            end = center + endPos,
            strokeWidth = config.scaleLineWidth.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun Int.position(
    angle: Float,
    scaleMultiplier: Float,
    radiusFactor: Float,
    minorRadiusFactor: Float
): Offset {
    val pointRadiusFactor = if (this % MAJOR_SCALE_MODULO == 0)
        radiusFactor
    else
        radiusFactor + minorRadiusFactor
    val scaledRadius = scaleMultiplier * pointRadiusFactor
    return Offset(cos(angle) * scaledRadius, sin(angle) * scaledRadius)
}

private val DialConfig.strokeCap: StrokeCap
    get() = if (roundCorners) StrokeCap.Round else StrokeCap.Butt

private fun DialJoinStyle.startAnglePadding(arcPadding: Float) = when (this) {
    is DialJoinStyle.Joined -> arcPadding
    is DialJoinStyle.Overlapped -> -2f * arcPadding
    is DialJoinStyle.WithDegreeGap -> degrees + arcPadding
}

private fun DialJoinStyle.sweepAnglePadding(arcPadding: Float) = when (this) {
    is DialJoinStyle.Joined -> 2f * arcPadding
    is DialJoinStyle.Overlapped -> -arcPadding
    is DialJoinStyle.WithDegreeGap -> degrees + (2f * arcPadding)
}

// The onboarding is copied from another app's source: its layout is expressed as raw pixel
// ratios, so the file level magic number rule is waived for it.
@file:Suppress("MagicNumber")

package com.example.myapplication.feature.main.impl.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositeShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.example.myapplication.ui.platform.theme.LocalAppIsDark

private val DarkSilkColor = Color(0xFF1C1C1C)
private const val DARK_SILK_CONTRAST = 1f
private val LightSilkColor = Color.White
private const val LIGHT_SILK_CONTRAST = 0.09f
private const val SILK_WAVE_SPEED = 0.15f
private const val SILK_WAVE_SCALE = 0.6f
private const val NANOS_PER_SECOND = 1_000_000_000f

internal class OnboardingSilk(
    private val silkShader: SilkShader,
    private val waveTime: MutableFloatState,
    private val baseColor: Color,
    private val contrast: Float,
) {
    fun currentShader(surfaceSize: Size): Shader? =
        silkShader.shaderFor(surfaceSize, waveTime.floatValue, SILK_WAVE_SCALE, baseColor, contrast)
}

@Composable
internal fun rememberOnboardingSilk(): OnboardingSilk {
    val silkShader = remember { SilkShader() }
    val waveTime = remember { mutableFloatStateOf(0f) }
    val isDark = LocalAppIsDark.current

    LaunchedEffect(silkShader) {
        if (!silkShader.isSupported) return@LaunchedEffect

        val startNanos = withFrameNanos { frameNanos -> frameNanos }
        while (true) {
            withFrameNanos { frameNanos ->
                waveTime.floatValue = (frameNanos - startNanos) / NANOS_PER_SECOND * SILK_WAVE_SPEED
            }
        }
    }

    return remember(silkShader, isDark) {
        OnboardingSilk(
            silkShader = silkShader,
            waveTime = waveTime,
            baseColor = if (isDark) DarkSilkColor else LightSilkColor,
            contrast = if (isDark) DARK_SILK_CONTRAST else LIGHT_SILK_CONTRAST,
        )
    }
}

@Composable
internal fun SilkBackdrop(silk: OnboardingSilk, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer()
            .drawBehind {
                val silkShader = silk.currentShader(size) ?: return@drawBehind
                drawRect(ShaderBrush(silkShader))
            },
    )
}

@Composable
internal fun StatementEdgeFades(
    silk: OnboardingSilk?,
    statementBounds: () -> Rect,
    topFade: Dp,
    bottomFade: Dp,
    fadeColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .graphicsLayer()
            .drawBehind {
                val bounds = statementBounds()
                if (bounds.height <= 0f) return@drawBehind

                val firstClearStop = (topFade.toPx() / bounds.height).coerceIn(0f, 0.5f)
                val lastClearStop = (1f - bottomFade.toPx() / bounds.height)
                    .coerceIn(firstClearStop, 1f)

                val silkShader = silk?.currentShader(size)
                val fadeBrush = if (silkShader != null) {
                    val alphaMask = LinearGradientShader(
                        from = Offset(0f, bounds.top),
                        to = Offset(0f, bounds.bottom),
                        colors = listOf(
                            Color.Black,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black,
                        ),
                        colorStops = listOf(0f, firstClearStop, lastClearStop, 1f),
                    )
                    ShaderBrush(
                        CompositeShader(
                            dst = silkShader,
                            src = alphaMask,
                            blendMode = BlendMode.DstIn,
                        ),
                    )
                } else {
                    val clearFadeColor = fadeColor.copy(alpha = 0f)
                    Brush.verticalGradient(
                        0f to fadeColor,
                        firstClearStop to clearFadeColor,
                        lastClearStop to clearFadeColor,
                        1f to fadeColor,
                        startY = bounds.top,
                        endY = bounds.bottom,
                    )
                }

                drawRect(brush = fadeBrush, topLeft = bounds.topLeft, size = bounds.size)
            },
    )
}

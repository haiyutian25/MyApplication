// The reel's choreography is expressed as raw pixel ratios and seconds, so the file level magic
// number rule is waived for it.
@file:Suppress("MagicNumber")

package com.example.myapplication.feature.settings.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.myapplication.ui.platform.components.ImmersiveTopBar
import com.example.myapplication.ui.platform.components.ImmersiveTopBarIconButton
import com.example.myapplication.ui.platform.theme.LocalAppIsDark
import com.example.myapplication.ui.platform.theme.MyApplicationTheme
import kotlin.math.hypot

/**
 * The word the particles assemble into: the name the app is listed under.
 */
private const val PARTICLE_WORD = "My Application"

/**
 * One loop of the reel, in seconds.
 */
private const val LOOP_SECONDS = 6.4f

/**
 * The time, in seconds, at which the iris starts closing over the frame.
 */
private const val CLOSE_START = 5.6f

/**
 * The fraction of the screen width the word spans.
 */
private const val TITLE_WIDTH_FRACTION = 0.86f

/**
 * The pixel size the word is measured at before it is scaled to fit the screen.
 */
private const val MEASURE_REFERENCE_PX = 200f

/**
 * The radius of the dot the iris closes onto, in hundredths of the screen width.
 */
private const val DOT_RADIUS = 1.6f

/**
 * How far the frame zooms out while the iris closes.
 */
private const val CLOSE_ZOOM = 0.35f

/**
 * How many nanoseconds one second holds.
 */
private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * The reel itself: a galaxy of particles flies into the word, inhales, detonates, and leaves the
 * solid word behind, looping every few seconds.
 *
 * The screen that shows it from the settings and the one the app opens with both draw this; each
 * adds what belongs to it over the top, and the reel owns only what is drawn.
 */
@Composable
internal fun ParticleReel(
    modifier: Modifier = Modifier,
    restartOnTap: Boolean = true,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val palette = particlePalette()
    val time = remember { mutableFloatStateOf(0f) }
    var restarts by remember { mutableIntStateOf(0) }

    LaunchedEffect(restarts) {
        val start = withFrameNanos { frameNanos -> frameNanos }
        while (true) {
            withFrameNanos { frameNanos ->
                time.floatValue = (frameNanos - start) / NANOS_PER_SECOND % LOOP_SECONDS
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (restartOnTap) {
                    Modifier.pointerInput(Unit) { detectTapGestures { restarts++ } }
                } else {
                    Modifier
                },
            ),
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val word = remember(width, height, density.density) {
            fittedWordLayout(measurer, density, width)
        }
        val field = remember(width, height, word, palette) {
            ParticleField(palette = palette).apply {
                prepare(width = width, height = height, layout = word, density = density)
            }
        }

        ParticleStage(
            field = field,
            word = word,
            palette = palette,
            time = time,
            unit = width / 100f,
        )
    }
}

/**
 * The screen that plays the particle text reel, opened from the settings: the reel, the way back,
 * and the hint that a tap starts it over.
 */
@Composable
fun ParticleTextScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ParticleReel()

        ImmersiveTopBar(
            leadingContent = { ParticleBackButton(onClick = onNavigateBack) },
        )

        Text(
            text = stringResource(id = R.string.particle_text_hint),
            style = MyApplicationTheme.typography.bodySmall,
            color = MyApplicationTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        )
    }
}

/**
 * The stage the reel is drawn on: the galaxy glow, the particle field, the solid word it leaves
 * behind, and the iris that closes the frame onto the dot the next loop bursts out of.
 *
 * [time] is read inside the draw pass rather than passed as a value, so that a frame only
 * invalidates the drawing of the stage instead of recomposing the screen around it.
 */
@Composable
private fun ParticleStage(
    field: ParticleField,
    word: TextLayoutResult,
    palette: ParticlePalette,
    time: FloatState,
    unit: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val seconds = time.floatValue
        val close = inOutExpo(progress(seconds, CLOSE_START, LOOP_SECONDS))
        val closeRadius = mix(hypot(size.width, size.height) / 2f, unit * DOT_RADIUS, close)
        val zoom = mix(1f, CLOSE_ZOOM, close)

        scale(scale = zoom, pivot = center) {
            drawGalaxyGlow(time = seconds, palette = palette)
            field.draw(scope = this, time = seconds, unit = unit)
            if (seconds >= IMPACT_AT) {
                drawSolidWord(word = word, palette = palette, time = seconds)
            }
        }

        if (close > 0f) {
            drawIrisClose(palette = palette, close = close, radius = closeRadius, unit = unit)
        }
    }
}

/**
 * The soft accent glow the galaxy is born out of at the top of the reel.
 */
private fun DrawScope.drawGalaxyGlow(time: Float, palette: ParticlePalette) {
    val glow = progress(time, 0f, 0.5f) * (1f - progress(time, 1.3f, 2.3f))
    if (glow <= 0f) return
    val radius = size.width * 0.35f
    drawCircle(
        brush = Brush.radialGradient(
            0f to palette.accent.copy(alpha = 0.45f * glow),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * The solid word that lands on the beat the particle cloud detonates on: a bloom of accent light,
 * the word slamming in, a highlight sweeping across it, and a one-frame flash over the frame.
 */
private fun DrawScope.drawSolidWord(
    word: TextLayoutResult,
    palette: ParticlePalette,
    time: Float,
) {
    val local = time - IMPACT_AT
    val origin = textOrigin(size = size, layout = word)
    val pivot = Offset(origin.x + word.size.width / 2f, origin.y + word.size.height / 2f)

    val bloom = outExpo(progress(local, 0f, 0.9f))
    if (bloom > 0f) {
        val radius = size.width * 0.8f * bloom + 1f
        drawCircle(
            brush = Brush.radialGradient(
                0f to palette.accent.copy(alpha = 0.35f),
                1f to Color.Transparent,
                center = pivot,
                radius = radius,
            ),
            radius = radius,
            center = pivot,
        )
    }

    val slam = outExpo(progress(local, 0f, 0.6f))
    scale(scale = mix(1.14f, 1f, slam), pivot = pivot) {
        drawText(textLayoutResult = word, color = palette.foreground, topLeft = origin)

        val sweep = progress(local, 0.4f, 1.1f)
        if (sweep > 0f && sweep < 1f) {
            val sweepX = mix(-0.3f * size.width, 1.3f * size.width, inOutCubic(sweep))
            val halfWidth = size.width * 0.12f
            drawText(
                textLayoutResult = word,
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to palette.accent,
                    1f to Color.Transparent,
                    start = Offset(sweepX - halfWidth, origin.y),
                    end = Offset(sweepX + halfWidth, origin.y + word.size.height),
                ),
                topLeft = origin,
            )
        }
    }

    val flash = 1f - progress(local, 0f, 0.16f)
    if (flash > 0f) {
        drawRect(color = palette.background, alpha = flash * 0.9f)
    }
}

/**
 * The mask that closes the frame onto a dot: everything outside the shrinking circle is painted in
 * the page colour, and the last of the circle flashes white so that the loop reads as one dot.
 */
private fun DrawScope.drawIrisClose(
    palette: ParticlePalette,
    close: Float,
    radius: Float,
    unit: Float,
) {
    val mask = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(-size.width, -size.height, size.width * 2f, size.height * 2f))
        addOval(Rect(center, radius))
    }
    drawPath(path = mask, color = palette.background)

    drawCircle(
        color = palette.accent,
        radius = radius + unit * 1.5f,
        center = center,
        alpha = 1f - close * close,
        style = Stroke(width = unit * 0.8f),
    )

    val flash = progress(close, 0.85f, 1f)
    if (flash > 0f) {
        drawCircle(color = palette.foreground, radius = radius, center = center, alpha = flash)
    }
}

@Composable
private fun ParticleBackButton(onClick: () -> Unit) {
    ImmersiveTopBarIconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(id = R.string.settings_navigate_back),
        )
    }
}

/**
 * The colours of the reel, taken from the app theme so that the screen changes with the theme like
 * every other screen. The additive blending of the trails is only kept for the dark colour scheme,
 * where overlapping trails read as light rather than as mud.
 */
@Composable
private fun particlePalette(): ParticlePalette {
    val scheme = MyApplicationTheme.colorScheme
    val isDark = LocalAppIsDark.current
    return remember(scheme, isDark) {
        ParticlePalette(
            background = scheme.background,
            foreground = scheme.onBackground,
            accent = scheme.primary,
            second = scheme.secondary,
            third = scheme.tertiary,
            additive = isDark,
        )
    }
}

/**
 * The layout of the word at the pixel size that makes it span [screenWidth] of the screen.
 */
private fun fittedWordLayout(
    measurer: TextMeasurer,
    density: Density,
    screenWidth: Float,
): TextLayoutResult {
    val reference = measurer.measure(
        text = PARTICLE_WORD,
        style = wordStyle(px = MEASURE_REFERENCE_PX, density = density),
        maxLines = 1,
        softWrap = false,
    )
    val px = MEASURE_REFERENCE_PX * screenWidth * TITLE_WIDTH_FRACTION / reference.size.width
    return measurer.measure(
        text = PARTICLE_WORD,
        style = wordStyle(px = px, density = density),
        maxLines = 1,
        softWrap = false,
    )
}

/**
 * The type the word is set in: the serif of the reel, with its caps optically centred on the stage.
 */
private fun wordStyle(px: Float, density: Density): TextStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = with(density) { px.toSp() },
    letterSpacing = (-0.02f).em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

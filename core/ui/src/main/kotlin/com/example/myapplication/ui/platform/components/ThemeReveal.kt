package com.example.myapplication.ui.platform.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot

/**
 * The curve the reveal moves on: the standard Material curve, which is what the reveal it is
 * taken from uses for this shape. A circle expands symmetrically rather than trailing off at the
 * end, so the curve is the symmetric one rather than the one its other variants use.
 */
private val revealEasing = CubicBezierEasing(
    a = 0.4f,
    b = 0f,
    c = 0.2f,
    d = 1f,
)

/**
 * How long the reveal takes, as in the reveal it is taken from.
 */
private const val REVEAL_DURATION_MILLIS = 700

/**
 * How far past the corner the circle reaches, as a fraction of the distance from the middle of
 * the element to its corner: the `circle(150%)` the reveal it is taken from ends on.
 */
private const val REVEAL_MAX_RADIUS_FRACTION = 0.75f

/**
 * The theme switch: a circle of [color] — the page of the theme being switched to — grows out of
 * the bottom middle of this element until it covers all of it, and [onCovered] is called at the
 * moment it does, which is when the theme itself is switched. The caller takes this element away
 * again from [onCovered], leaving the new theme on screen.
 *
 * A circle growing from an edge is a reveal, not decoration: it is what a theme switch that keeps
 * the page in place looks like. Pass [animate] as false to skip it and let [onCovered] run at
 * once, which is what the app does when animations are switched off system-wide.
 */
@Composable
fun ThemeReveal(
    color: Color,
    onCovered: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    var revealedSize by remember { mutableStateOf(IntSize.Zero) }
    val progress = remember { Animatable(initialValue = if (animate) 0f else 1f) }

    LaunchedEffect(key1 = animate) {
        if (animate) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REVEAL_DURATION_MILLIS,
                    easing = revealEasing,
                ),
            )
        }
        onCovered()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { revealedSize = it }
            .drawWithContent {
                if (revealedSize == IntSize.Zero) return@drawWithContent

                val origin = Offset(
                    x = revealedSize.width / 2f,
                    y = revealedSize.height.toFloat(),
                )
                val maxRadius = REVEAL_MAX_RADIUS_FRACTION * hypot(
                    revealedSize.width.toFloat(),
                    revealedSize.height.toFloat(),
                )
                val circle = Path().apply {
                    addOval(
                        oval = Rect(center = origin, radius = maxRadius * progress.value),
                    )
                }

                clipPath(path = circle) {
                    drawRect(color = color)
                }
            },
    )
}

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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
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
 * How long the reveal takes. The reveal it is taken from takes 700ms; this app runs it slower on
 * purpose, so that the circle is seen to travel across the page rather than snap over it.
 */
private const val REVEAL_DURATION_MILLIS = 7000

/**
 * The theme switch, the way a view transition does it: a circle opens out of the bottom middle of
 * this element onto the page of the theme being switched to, which is the page the app itself is
 * already drawing behind this element, so the new page is seen through the circle as it grows —
 * its words, its icons and all — rather than being painted over and then swapped in.
 *
 * What this element draws is the other way round: [previousPage], the page as it looked before the
 * switch, over everything the circle has not reached yet. So the page appears to be opened, not
 * covered: inside the circle is the new page, outside it the old one.
 *
 * [onCovered] is called once the circle has covered all of the element. By then the old page has
 * been drawn over completely, so the caller takes this element away and what is left on screen is
 * the new page, already drawn in full. Whether a reveal is shown at all is decided where the theme
 * is applied, which leaves this out when animations are switched off system-wide.
 */
@Composable
fun ThemeReveal(
    previousPage: ImageBitmap,
    onCovered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealedSize by remember { mutableStateOf(IntSize.Zero) }
    val progress = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(key1 = Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = REVEAL_DURATION_MILLIS,
                easing = revealEasing,
            ),
        )
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
                // The circle has to reach the furthest corner of the element to cover all of it,
                // and it starts at the bottom middle: from there that corner is half the width
                // across and the whole height up. (The reveal this is taken from reaches 150% of
                // the distance from the middle of the element to its corner, which is enough for
                // the origin it defaults to but not for one on an edge: a circle that stops at
                // that radius leaves the far edge of a tall screen uncovered.)
                val maxRadius = hypot(
                    revealedSize.width / 2f,
                    revealedSize.height.toFloat(),
                )
                val circle = Path().apply {
                    addOval(
                        oval = Rect(center = origin, radius = maxRadius * progress.value),
                    )
                }

                clipPath(path = circle, clipOp = ClipOp.Difference) {
                    drawImage(
                        image = previousPage,
                        srcSize = IntSize(previousPage.width, previousPage.height),
                        dstSize = IntSize(revealedSize.width, revealedSize.height),
                    )
                }
            },
    )
}

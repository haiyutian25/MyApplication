// The onboarding is copied from another app's source: its layout is expressed as raw pixel
// ratios, so the file level magic number rule is waived for it.
@file:Suppress("MagicNumber")

package com.example.myapplication.feature.main.impl.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.myapplication.feature.main.impl.R
import com.example.myapplication.ui.platform.components.mascot.JellyBobView
import kotlin.random.Random

private val WideLayoutMinWidth = 660.dp
private val WidePaneMaxWidth = 720.dp
private val CompactPaneMaxWidth = 520.dp

/**
 * The side of the square the mascot of the onboarding is drawn in, and its distance to the edges of
 * the screen. The mascot places its antennae and its shadow around its body, so it needs a square
 * area of its own.
 */
private val OnboardingMascotSize = 96.dp
private val OnboardingMascotStartMargin = 24.dp

/**
 * How far the top of the onboarding is pushed down as a whole: the mascot and the statements that
 * have drifted upwards and are fading out as the flow advances come to rest just under the mascot,
 * so both move down by this much and the pair sits clear of the status bar.
 *
 * The statements are moved by placing them lower, not by giving their area a smaller size: the
 * edge fades are a fraction of that area's height, so shrinking it would change the fade (and the
 * blur it leaves behind) away from the look the flow is ported from.
 */
private val OnboardingTopContentOffset = 32.dp
private val OnboardingMascotTopMargin = OnboardingMascotStartMargin + OnboardingTopContentOffset
private val WideSidePadding = 56.dp
private val CompactSidePadding = 28.dp
private val WidePanelMaxWidth = 560.dp
private val WideButtonWidth = 300.dp
private val WideGapBelowStatements = 12.dp
private val CompactGapBelowStatements = 8.dp
private val WideGapAboveButton = 48.dp
private val CompactGapAboveButton = 40.dp
private val WideButtonGap = 16.dp
private val WideBottomMargin = 68.dp
private val MobileBottomMargin = 30.dp
private val WideSpaceBetweenStatements = 76.dp
private val CompactSpaceBetweenStatements = 54.dp
private val ControlHeight = 54.dp
private val SkipTapPadding = 12.dp
private val MobileSkipInset = 14.dp
private val SkipLabelReserve = 22.dp
private val MobileSystemBarsReserve = 48.dp
private val WideMinStatementSpace = 160.dp
private val CompactMinStatementSpace = 175.dp
private val WideTopFadeHeight = 120.dp
private val WideBottomFadeHeight = 32.dp
private val CompactTopFadeHeight = 96.dp
private val CompactBottomFadeHeight = 28.dp
private const val SHOW_TEXTURED_BACKGROUND = false
private val GrainCellSize = 1.dp
private const val GRAIN_TILE_CELLS = 128
private const val GRAIN_DENSITY = 0.20f
private const val GRAIN_MIN_ALPHA = 0.02f
private const val GRAIN_MAX_ALPHA = 0.06f
private const val GRAIN_SEED = 7

internal val LocalOnboardingWideLayout = compositionLocalOf { false }

/**
 * The frame a step is shown in: the statements glide through the middle, the panel and the
 * button sit under them, and the skip label and the ghost sit at the top.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // One layout, laid out in one place.
@Composable
internal fun OnboardingFlowScaffold(
    statements: List<OnboardingStep>,
    currentStepIndex: Int,
    showSkip: Boolean,
    onSkip: () -> Unit,
    onSwipeForward: (() -> Unit)?,
    onSwipeBackward: (() -> Unit)?,
    panel: @Composable ColumnScope.() -> Unit,
    bottomBar: @Composable () -> Unit,
    showOnboardingMascot: Boolean = false,
) {
    val grainBrush = rememberGrainBrush(speckColor = MaterialTheme.colorScheme.onBackground)
    val silk = if (SHOW_TEXTURED_BACKGROUND) rememberOnboardingSilk() else null
    var scaffoldOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var statementStackBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { scaffold -> scaffoldOriginInRoot = scaffold.positionInRoot() }
            .drawWithContent {
                drawContent()
                if (SHOW_TEXTURED_BACKGROUND) drawRect(grainBrush)
            },
    ) {
        val isWideLayout = maxWidth >= WideLayoutMinWidth

        val paneMaxWidth = if (isWideLayout) WidePaneMaxWidth else CompactPaneMaxWidth
        val sidePadding = if (isWideLayout) WideSidePadding else CompactSidePadding
        val panelMaxWidth = if (isWideLayout) WidePanelMaxWidth else CompactPaneMaxWidth
        val panelSidePadding = if (isWideLayout) 0.dp else sidePadding
        val gapAboveButton = if (isWideLayout) WideGapAboveButton else CompactGapAboveButton
        val skipInset = MobileSkipInset
        val systemBarsReserve = MobileSystemBarsReserve
        val gapBelowStatements = if (isWideLayout) {
            WideGapBelowStatements
        } else {
            CompactGapBelowStatements
        }
        val bottomMargin = if (isWideLayout) WideBottomMargin else MobileBottomMargin
        val spaceBetweenStatements = if (isWideLayout) {
            WideSpaceBetweenStatements
        } else {
            CompactSpaceBetweenStatements
        }
        val topFadeHeight = if (isWideLayout) WideTopFadeHeight else CompactTopFadeHeight
        val bottomFadeHeight = if (isWideLayout) {
            WideBottomFadeHeight
        } else {
            CompactBottomFadeHeight
        }
        val minStatementSpace = if (isWideLayout) {
            WideMinStatementSpace
        } else {
            CompactMinStatementSpace
        }

        val topRowReserve = skipInset + SkipTapPadding * 2 + SkipLabelReserve
        val bottomBarReserve = gapAboveButton + ControlHeight + bottomMargin
        val panelMaxHeight = (
            maxHeight - topRowReserve - bottomBarReserve - gapBelowStatements -
                systemBarsReserve - minStatementSpace
            ).coerceAtLeast(0.dp)

        CompositionLocalProvider(LocalOnboardingWideLayout provides isWideLayout) {
            if (silk != null) {
                SilkBackdrop(silk = silk, modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (onSwipeForward != null && onSwipeBackward != null) {
                            Modifier.swipeBetweenSteps(
                                stepKey = currentStepIndex,
                                onSwipeForward = onSwipeForward,
                                onSwipeBackward = onSwipeBackward,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                OnboardingTopRow(showSkip = showSkip, onSkip = onSkip, skipInset = skipInset)

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = paneMaxWidth)
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    OnboardingStatementStack(
                        steps = statements,
                        currentStepIndex = currentStepIndex,
                        spaceBetweenStatements = spaceBetweenStatements,
                        bottomFadeHeight = bottomFadeHeight,
                        isWideLayout = isWideLayout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = sidePadding)
                            .offset(y = OnboardingTopContentOffset)
                            .onGloballyPositioned { statementStack ->
                                statementStackBoundsInRoot = statementStack.boundsInRoot()
                            },
                    )

                    Spacer(modifier = Modifier.height(gapBelowStatements))

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .widthIn(max = panelMaxWidth)
                            .fillMaxWidth()
                            .heightIn(max = panelMaxHeight)
                            .padding(horizontal = panelSidePadding),
                        content = panel,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = sidePadding,
                                end = sidePadding,
                                top = gapAboveButton,
                                bottom = bottomMargin,
                            ),
                    ) {
                        bottomBar()
                    }
                }
            }

            StatementEdgeFades(
                silk = silk,
                statementBounds = { statementStackBoundsInRoot.translate(-scaffoldOriginInRoot) },
                topFade = topFadeHeight,
                bottomFade = bottomFadeHeight,
                fadeColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            )

            if (showOnboardingMascot) {
                AndroidView(
                    factory = { context -> JellyBobView(context) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = OnboardingMascotStartMargin,
                            top = OnboardingMascotTopMargin,
                        )
                        .size(size = OnboardingMascotSize),
                )
            }
        }
    }
}

@Composable
private fun rememberGrainBrush(speckColor: Color): ShaderBrush {
    val cellSizePx = with(LocalDensity.current) { GrainCellSize.roundToPx() }.coerceAtLeast(1)

    return remember(cellSizePx, speckColor) {
        ShaderBrush(
            ImageShader(
                image = createGrainTile(cellSizePx, speckColor),
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            ),
        )
    }
}

private fun createGrainTile(cellSizePx: Int, speckColor: Color): ImageBitmap {
    val tileSizePx = GRAIN_TILE_CELLS * cellSizePx
    val tile = ImageBitmap(tileSizePx, tileSizePx)
    val canvas = Canvas(tile)
    val paint = Paint()
    val random = Random(GRAIN_SEED)

    for (row in 0 until GRAIN_TILE_CELLS) {
        for (column in 0 until GRAIN_TILE_CELLS) {
            if (random.nextFloat() >= GRAIN_DENSITY) continue

            val speckAlpha = GRAIN_MIN_ALPHA +
                random.nextFloat() * (GRAIN_MAX_ALPHA - GRAIN_MIN_ALPHA)
            paint.color = speckColor.copy(alpha = speckAlpha)

            val left = (column * cellSizePx).toFloat()
            val top = (row * cellSizePx).toFloat()
            canvas.drawRect(left, top, left + cellSizePx, top + cellSizePx, paint)
        }
    }

    return tile
}

@Composable
internal fun OnboardingFlowBottomBar(
    label: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val isWideLayout = LocalOnboardingWideLayout.current

    Box(modifier = Modifier.fillMaxWidth()) {
        if (isWideLayout) {
            val backButtonOffset =
                -(WideButtonWidth / 2 + WideButtonGap + ControlHeight / 2)

            OnboardingPillButton(
                label = label,
                onClick = onNext,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(WideButtonWidth),
            )

            AnimatedVisibility(
                visible = showBack,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = backButtonOffset),
                enter = fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.8f),
                exit = fadeOut(tween(170)) + scaleOut(tween(170), targetScale = 0.8f),
            ) {
                OnboardingStepBackButton(onClick = onBack)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = showBack,
                    enter = fadeIn(tween(300, delayMillis = 160)) + expandHorizontally(
                        animationSpec = tween(400, easing = ONBOARDING_GLIDE_EASING),
                        expandFrom = Alignment.Start,
                    ),
                    exit = fadeOut(tween(140)) + shrinkHorizontally(
                        animationSpec = tween(340, easing = ONBOARDING_GLIDE_EASING),
                        shrinkTowards = Alignment.Start,
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OnboardingStepBackButton(onClick = onBack)

                        Spacer(modifier = Modifier.width(14.dp))
                    }
                }

                OnboardingPillButton(
                    label = label,
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OnboardingTopRow(showSkip: Boolean, onSkip: () -> Unit, skipInset: Dp) {
    val skipAlpha by animateFloatAsState(
        targetValue = if (showSkip) 1f else 0f,
        animationSpec = tween(260),
        label = "onboarding-skip-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(end = skipInset, top = skipInset),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_skip),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier
                .graphicsLayer { alpha = skipAlpha }
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = showSkip, onClick = onSkip)
                .padding(SkipTapPadding),
        )
    }
}

@Composable
private fun OnboardingPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(ControlHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = label,
            animationSpec = tween(450),
            label = "onboarding-button-label",
        ) { visibleLabel ->
            Text(
                text = visibleLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun OnboardingStepBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ControlHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.onboarding_chevron_left),
            contentDescription = "Go back",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun Modifier.swipeBetweenSteps(
    stepKey: Int,
    onSwipeForward: () -> Unit,
    onSwipeBackward: () -> Unit,
): Modifier = pointerInput(stepKey) {
    val swipeThreshold = 72.dp.toPx()
    var horizontalDragTotal = 0f

    detectHorizontalDragGestures(
        onDragStart = { horizontalDragTotal = 0f },
        onDragCancel = { horizontalDragTotal = 0f },
        onDragEnd = {
            if (horizontalDragTotal <= -swipeThreshold) onSwipeForward()
            if (horizontalDragTotal >= swipeThreshold) onSwipeBackward()
            horizontalDragTotal = 0f
        },
        onHorizontalDrag = { _, dragAmount -> horizontalDragTotal += dragAmount },
    )
}

@Composable
internal fun <T> OnboardingPanelTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val crossFade = fadeIn(tween(500, delayMillis = 800)) togetherWith fadeOut(tween(300))

            crossFade using SizeTransform { _, _ ->
                tween(ONBOARDING_GLIDE_DURATION_MILLIS, easing = ONBOARDING_GLIDE_EASING)
            }
        },
        label = "onboarding-panel",
    ) { visibleState ->
        content(visibleState)
    }
}

package com.example.myapplication.ui.platform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.platform.theme.MyApplicationTheme

/**
 * The height of the floating button row of [ImmersiveTopBar], excluding the status bar inset.
 */
val ImmersiveTopBarHeight: Dp = 56.dp

/**
 * The padding that screen content should reserve at the top so that its first items are not
 * covered by [ImmersiveTopBar].
 *
 * Applying this as content padding (rather than as a layout modifier) keeps the content itself
 * edge-to-edge, so it can still scroll beneath the floating buttons.
 *
 * @param extraTop Additional space below the floating buttons, for screens whose first item would
 * otherwise sit directly against them.
 */
@Composable
fun immersiveTopBarContentPadding(extraTop: Dp = 0.dp): PaddingValues = PaddingValues(
    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        ImmersiveTopBarHeight +
        extraTop,
)

/**
 * A transparent top bar that floats above the screen content.
 *
 * Independent buttons are anchored to the start and end edges of the screen, with no background,
 * title, or divider in between, so the content can extend all the way to the top of the screen
 * beneath them. Use [immersiveTopBarContentPadding] to keep the first items of the content out
 * from under the buttons.
 *
 * This must be called from within a [BoxScope] so that it can be aligned to the top of the
 * content it floats above.
 */
@Composable
fun BoxScope.ImmersiveTopBar(
    modifier: Modifier = Modifier,
    leadingContent: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = leadingContent,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = trailingContent,
        )
    }
}

/**
 * The Material 3 [OutlinedIconButton] used for the actions of [ImmersiveTopBar].
 *
 * The button follows the article cards of the blog this app's visual design is based on: a pure
 * white container wrapped in a hairline outline. [OutlinedIconButton] is the Material 3 variant
 * that exposes an official `border` parameter, which is what makes the outline possible.
 *
 * The Material 3 baseline color scheme colors a tonal button with the purple-toned
 * `secondaryContainer` role, which stands out against the neutral page background of this app. In
 * light mode the container therefore uses `surfaceContainerLowest`, which resolves to pure white.
 * Dark mode keeps the Material 3 defaults untouched.
 */
@Composable
fun ImmersiveTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isLight = isLightColorScheme()
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = immersiveTopBarIconButtonColors(isLight = isLight),
        border = immersiveTopBarIconButtonBorder(),
        content = content,
    )
}

/**
 * The hairline width of the [ImmersiveTopBarIconButton] outline, shared with the outline of
 * [SettingsGroup] and matching the article cards this app's visual design is based on.
 */
private val TopBarBorderWidth: Dp = 1.dp

/**
 * Surfaces lighter than this are treated as belonging to a light color scheme. The app ships two
 * fixed color schemes and never derives colors from the wallpaper, so the luminance of the surface
 * is a reliable signal.
 */
private const val LIGHT_SURFACE_LUMINANCE = 0.5f

/**
 * Whether the current color scheme is a light one.
 */
@Composable
private fun isLightColorScheme(): Boolean =
    MyApplicationTheme.colorScheme.surface.luminance() > LIGHT_SURFACE_LUMINANCE

/**
 * The colors of [ImmersiveTopBarIconButton]: pure white on a light color scheme, and the Material 3
 * tonal defaults on a dark one.
 */
@Composable
private fun immersiveTopBarIconButtonColors(isLight: Boolean): IconButtonColors {
    val colorScheme = MyApplicationTheme.colorScheme
    return if (isLight) {
        IconButtonDefaults.outlinedIconButtonColors(
            containerColor = colorScheme.surfaceContainerLowest,
            contentColor = colorScheme.onSurface,
        )
    } else {
        IconButtonDefaults.outlinedIconButtonColors(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
        )
    }
}

/**
 * The outline of [ImmersiveTopBarIconButton]. It draws from the `outlineVariant` color role, which
 * the theme defines, so that every floating panel of the app is outlined with the same hairline.
 */
@Composable
private fun immersiveTopBarIconButtonBorder(): BorderStroke = BorderStroke(
    width = TopBarBorderWidth,
    color = MyApplicationTheme.colorScheme.outlineVariant,
)

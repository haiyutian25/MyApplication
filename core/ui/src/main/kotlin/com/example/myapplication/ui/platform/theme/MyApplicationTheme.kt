package com.example.myapplication.ui.platform.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.platform.theme.shape.MyApplicationShapes
import com.example.myapplication.ui.platform.theme.type.MyApplicationTypography

/**
 * The color scheme used when the app is displayed in light mode: the Material 3 baseline color
 * scheme with the page background, the surface, and the colors of the floating panels overridden.
 */
private val LightColorScheme: ColorScheme = lightColorScheme(
    background = Color(color = 0xFFF7F9FE),
    surface = Color(color = 0xFFF7F9FE),
    // The grouped settings panels read as raised white cards on the off-white page. This is the
    // color role that Material 3 reserves for such low-elevation containers.
    surfaceContainerLow = Color(color = 0xFFFFFFFF),
    // Menus and other pop-ups are white as well. This is the color role that Material 3 gives to
    // them by default, so the components that open one do not have to override their colors.
    surfaceContainer = Color(color = 0xFFFFFFFF),
    // The hairline outline shared by every floating panel of the app: the top bar buttons and the
    // grouped settings cards. Material 3 uses this role for decorative borders and dividers.
    outlineVariant = Color(color = 0xFFE3E8F7),
)

/**
 * The color scheme used when the app is displayed in dark mode. Each value is a dark-mode value of
 * the site this app's visual design is based on, put in the color role Material 3 defines for it.
 */
private val DarkColorScheme: ColorScheme = darkColorScheme(
    // primary is the accent that fills the key components (buttons, selection and active states),
    // and onPrimary is the content drawn on top of it.
    primary = Color(color = 0xFF7454FC),
    onPrimary = Color(color = 0xFFFFFFFF),
    // background and onBackground are the page and the content drawn on it. Material 3 keeps the
    // role for compatibility and points it at surface in newer versions of the spec.
    background = Color(color = 0xFF17191D),
    onBackground = Color(color = 0xFFF7F7FA),
    // surface is the surface the page is drawn on, onSurface its high-emphasis content.
    surface = Color(color = 0xFF17191D),
    onSurface = Color(color = 0xFFF7F7FA),
    // The container levels Material 3 defines for the surfaces that sit on the page: the grouped
    // panels of the app sit on the low level, the menus that open over them on the default one.
    surfaceContainerLow = Color(color = 0xFF1B1C20),
    surfaceContainer = Color(color = 0xFF1B1C20),
    // The decorative hairline that Material 3 gives to dividers and to the outline of the floating
    // panels of the app: the top bar buttons and the grouped settings cards.
    outlineVariant = Color(color = 0xFF3D3D3F),
)

/**
 * The colour the page is painted with when the app is shown in [isDark] mode.
 *
 * It is what the theme switch puts on screen before the theme itself changes: the reveal draws the
 * page of the theme it is switching to, one circle of it at a time.
 */
fun pageBackgroundColor(isDark: Boolean): Color =
    if (isDark) DarkColorScheme.background else LightColorScheme.background

/**
 * Static wrapper to make accessing the theme components easier.
 */
object MyApplicationTheme {
    /**
     * Retrieves the current [ColorScheme].
     */
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    /**
     * Retrieves the current [Typography].
     */
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    /**
     * Retrieves the current [Shapes].
     */
    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}

/**
 * The top-level theme for the app, responsible for providing the color scheme, typography, and
 * shapes to the Compose hierarchy, and for painting the page background.
 *
 * Colors come from the Material 3 baseline color scheme and are never derived from the user's
 * wallpaper. The page background is painted here rather than by each screen, so that every screen
 * switches color together with the theme.
 */
/**
 * Whether the app is currently displayed in dark mode.
 *
 * The color scheme already carries the colors of both modes, so this is only for the few places
 * that need to know the mode itself: the mascot of the app is painted black or white rather than in
 * a color role, which is how the mascot is drawn in the Emberr app this app takes it from.
 */
val LocalAppIsDark = staticCompositionLocalOf { false }

/**
 * The top-level theme for the app, responsible for providing the color scheme, typography, and
 * shapes to the Compose hierarchy, and for painting the page background.
 *
 * Colors come from the Material 3 baseline color scheme and are never derived from the user's
 * wallpaper. The page background is painted here rather than by each screen, so that every screen
 * switches color together with the theme.
 */
@Composable
fun MyApplicationTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme,
        typography = MyApplicationTypography,
        shapes = MyApplicationShapes,
    ) {
        CompositionLocalProvider(LocalAppIsDark provides isDarkMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MyApplicationTheme.colorScheme.background,
                content = content,
            )
        }
    }
}

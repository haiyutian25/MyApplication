package com.example.myapplication.ui.platform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.platform.theme.MyApplicationTheme

/**
 * The distance from the start of a [SettingsGroupRow] to the start of its title: the horizontal
 * content padding of a list item, the width of its leading icon, and the gap between the two.
 *
 * [SettingsGroupDivider] is indented by this amount so that dividers line up with the row titles
 * instead of cutting through the leading icons.
 */
private val SettingsGroupTitleStart: Dp = 56.dp

/**
 * The horizontal space between the edges of the screen and a [SettingsGroup].
 */
private val SettingsGroupHorizontalPadding: Dp = 16.dp

/**
 * The hairline width of the [SettingsGroup] outline, matching the outline of the floating top bar
 * buttons and of the article cards of the design this app follows.
 */
private val SettingsGroupBorderWidth: Dp = 1.dp

/**
 * The distance from the leading edge of a [SettingsGroup] to its title, so that the title is inset
 * slightly from the edge of the panel below it.
 */
private val SettingsGroupTitleInset: Dp = 6.dp

/**
 * The space between the title of a [SettingsGroup] and the panel below it.
 */
private val SettingsGroupTitleSpacing: Dp = 10.dp

/**
 * A card that groups related settings entries into a single raised panel, introduced by a title
 * that labels what the group is about. A panel that speaks for itself is drawn without one, by
 * leaving the title out.
 *
 * The panel uses the `surfaceContainerLow` color role, so it stands out against the page background
 * the same way a card does on the page background of the design this app follows. It is wrapped in
 * the same hairline outline as the floating top bar buttons, taken from the `outlineVariant` role,
 * and it casts no shadow: the color difference and the outline are what separate it from the page.
 *
 * Separate groups with a vertical gap of about 22dp rather than with dividers, and use
 * [SettingsGroupDivider] between the rows *within* one group.
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsGroupHorizontalPadding),
    ) {
        if (title != null) {
            SettingsGroupTitle(text = title)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MyApplicationTheme.shapes.large,
            color = MyApplicationTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = SettingsGroupBorderWidth,
                color = MyApplicationTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(content = content)
        }
    }
}

/**
 * The small label above the panel of a [SettingsGroup], which tells the user what the entries of
 * the group have in common.
 */
@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MyApplicationTheme.typography.labelSmall,
        color = MyApplicationTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = SettingsGroupTitleInset,
            bottom = SettingsGroupTitleSpacing,
        ),
    )
}

/**
 * A divider between two rows of a [SettingsGroup].
 *
 * It is indented so that it lines up with the row titles. There is no divider above the first
 * row or below the last row of a group, so dividers never run into the rounded corners of it.
 */
@Composable
fun SettingsGroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(
            start = SettingsGroupTitleStart,
            end = SettingsGroupHorizontalPadding,
        ),
    )
}

/**
 * A single entry of a [SettingsGroup], laid out as a Material 3 [ListItem]: a leading icon, a
 * title, and optional trailing content.
 *
 * The list item is drawn on a transparent container so that the background of its [SettingsGroup]
 * shows through instead of stacking a second panel inside the first one.
 *
 * @param onClick Makes the whole row clickable.
 * @param interactionSource The source that a control inside `trailingContent` can observe to react
 * to presses on the row. Pass the same source to `Modifier.indication` there to draw the press
 * feedback next to the row instead of across it.
 * @param showRipple Whether clicking the row draws a ripple across the whole row. Pass false when
 * the feedback belongs to a smaller control inside it, such as the arrow that expands a menu; the
 * row stays clickable either way.
 * @param trailingContent Shown at the end of the row, such as the current value of the setting
 * or an arrow that indicates navigation.
 */
@Composable
fun SettingsGroupRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    showRipple: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val rowInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    ListItem(
        headlineContent = { Text(text = title) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = when {
            onClick == null -> modifier
            showRipple -> modifier.clickable(onClick = onClick)
            else -> modifier.clickable(
                interactionSource = rowInteractionSource,
                indication = null,
                onClick = onClick,
            )
        },
    )
}

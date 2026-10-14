// The panel is copied from another app's source: its timings and sizes are raw numbers, so the
// file level magic number rule is waived for it.
@file:Suppress("MagicNumber")

package com.example.myapplication.feature.main.impl.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.feature.main.impl.R

/**
 * The panel a step shows under the statements: the choice that step asks for, or nothing at all.
 */
@Composable
internal fun OnboardingChoicePanel(
    choice: OnboardingChoice,
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
) {
    OnboardingPanelTransition(targetState = choice, modifier = modifier) { visibleChoice ->
        when (visibleChoice) {
            OnboardingChoice.NONE -> Spacer(modifier = Modifier.fillMaxWidth())
            OnboardingChoice.THEME -> ThemeChoice(viewModel)
        }
    }
}

/**
 * The one choice this app's onboarding asks for: the theme the app is drawn in.
 */
@Composable
private fun ThemeChoice(viewModel: OnboardingViewModel) {
    val selectedTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTheme.entries.forEach { theme ->
            ChoiceOptionRow(
                label = theme.displayName(),
                isSelected = theme == selectedTheme,
                onClick = { viewModel.setAppTheme(theme) },
            )
        }
    }
}

@Composable
private fun ChoiceOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = optionColorFor(isSelected),
        animationSpec = tween(220),
        label = "onboarding-option-color",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.6f),
            exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.6f),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.onboarding_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun optionColorFor(isSelected: Boolean): Color = if (isSelected) {
    MaterialTheme.colorScheme.onBackground
} else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
}

private fun AppTheme.displayName(): String = when (this) {
    AppTheme.DEFAULT -> "Follow the system"
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
}

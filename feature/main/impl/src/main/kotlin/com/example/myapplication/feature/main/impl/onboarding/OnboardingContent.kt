package com.example.myapplication.feature.main.impl.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myapplication.feature.main.impl.R

/**
 * The kinds of choice a step can ask the user for.
 */
internal enum class OnboardingChoice {
    NONE,
    THEME,
}

/**
 * One step of the onboarding: what it says, and the choice it asks for.
 */
internal data class OnboardingStep(
    @DrawableRes val icon: Int,
    val lead: String,
    val headline: String,
    val detail: String? = null,
    val choice: OnboardingChoice = OnboardingChoice.NONE,
    val buttonLabel: String,
    val hasRainbowHeadline: Boolean = false,
)

/**
 * The steps the onboarding walks through, in order: what the app is, what it does not do, where the
 * mascots live, the one choice it asks for, and the send-off.
 *
 * Every word of them comes from the string resources, so the flow is read in whatever language the
 * app is currently shown in.
 */
@Composable
internal fun rememberOnboardingSteps(): List<OnboardingStep> = buildList {
    add(
        OnboardingStep(
            icon = R.drawable.onboarding_pen_square,
            lead = stringResource(id = R.string.onboarding_welcome_lead),
            headline = stringResource(id = R.string.onboarding_welcome_headline),
            buttonLabel = stringResource(id = R.string.onboarding_welcome_action),
        ),
    )
    add(
        OnboardingStep(
            icon = R.drawable.onboarding_shield_alert,
            lead = stringResource(id = R.string.onboarding_privacy_lead),
            headline = stringResource(id = R.string.onboarding_privacy_headline),
            detail = stringResource(id = R.string.onboarding_privacy_detail),
            buttonLabel = stringResource(id = R.string.onboarding_continue),
        ),
    )
    add(
        OnboardingStep(
            icon = R.drawable.onboarding_widget,
            lead = stringResource(id = R.string.onboarding_mascots_lead),
            headline = stringResource(id = R.string.onboarding_mascots_headline),
            detail = stringResource(id = R.string.onboarding_mascots_detail),
            buttonLabel = stringResource(id = R.string.onboarding_continue),
        ),
    )
    add(
        OnboardingStep(
            icon = R.drawable.onboarding_sliders_horizontal,
            lead = stringResource(id = R.string.onboarding_theme_lead),
            headline = stringResource(id = R.string.onboarding_theme_headline),
            choice = OnboardingChoice.THEME,
            buttonLabel = stringResource(id = R.string.onboarding_continue),
        ),
    )
    add(
        OnboardingStep(
            icon = R.drawable.onboarding_square_check,
            lead = stringResource(id = R.string.onboarding_done_lead),
            headline = stringResource(id = R.string.onboarding_done_headline),
            detail = stringResource(id = R.string.onboarding_done_detail),
            buttonLabel = stringResource(id = R.string.onboarding_done_action),
            hasRainbowHeadline = true,
        ),
    )
}

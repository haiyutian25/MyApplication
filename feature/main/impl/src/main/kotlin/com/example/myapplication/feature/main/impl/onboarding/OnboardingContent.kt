package com.example.myapplication.feature.main.impl.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
 *
 * The words of a step are held as string resources rather than as text, so that the flow is read in
 * whatever language the app is currently shown in.
 */
internal data class OnboardingStep(
    @DrawableRes val icon: Int,
    @StringRes val leadRes: Int,
    @StringRes val headlineRes: Int,
    @StringRes val detailRes: Int? = null,
    val choice: OnboardingChoice = OnboardingChoice.NONE,
    @StringRes val buttonLabelRes: Int,
    val hasRainbowHeadline: Boolean = false,
)

/**
 * The steps the onboarding walks through, in order: what the app is, what it does not do, where the
 * mascots live, the one choice it asks for, and the send-off.
 */
internal fun onboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        icon = R.drawable.onboarding_pen_square,
        leadRes = R.string.onboarding_welcome_lead,
        headlineRes = R.string.onboarding_welcome_headline,
        buttonLabelRes = R.string.onboarding_welcome_action,
    ),
    OnboardingStep(
        icon = R.drawable.onboarding_shield_alert,
        leadRes = R.string.onboarding_privacy_lead,
        headlineRes = R.string.onboarding_privacy_headline,
        detailRes = R.string.onboarding_privacy_detail,
        buttonLabelRes = R.string.onboarding_continue,
    ),
    OnboardingStep(
        icon = R.drawable.onboarding_widget,
        leadRes = R.string.onboarding_mascots_lead,
        headlineRes = R.string.onboarding_mascots_headline,
        detailRes = R.string.onboarding_mascots_detail,
        buttonLabelRes = R.string.onboarding_continue,
    ),
    OnboardingStep(
        icon = R.drawable.onboarding_sliders_horizontal,
        leadRes = R.string.onboarding_theme_lead,
        headlineRes = R.string.onboarding_theme_headline,
        choice = OnboardingChoice.THEME,
        buttonLabelRes = R.string.onboarding_continue,
    ),
    OnboardingStep(
        icon = R.drawable.onboarding_square_check,
        leadRes = R.string.onboarding_done_lead,
        headlineRes = R.string.onboarding_done_headline,
        detailRes = R.string.onboarding_done_detail,
        buttonLabelRes = R.string.onboarding_done_action,
        hasRainbowHeadline = true,
    ),
)

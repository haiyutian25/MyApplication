package com.example.myapplication.feature.main.impl.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * The onboarding: one flow of steps, where the statements glide past and the panel under them asks
 * for the choice that the current step belongs to.
 */
@Composable
internal fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val steps = rememberOnboardingSteps()

    var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
    var isFinishing by remember { mutableStateOf(false) }

    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.lastIndex

    fun finishOnboarding() {
        if (isFinishing) return
        isFinishing = true
        viewModel.completeOnboarding()
        onFinished()
    }

    fun goToNextStep() {
        if (isLastStep) finishOnboarding() else currentStepIndex++
    }

    fun goToPreviousStep() {
        if (currentStepIndex > 0) currentStepIndex--
    }

    OnboardingFlowScaffold(
        statements = steps,
        currentStepIndex = currentStepIndex,
        showSkip = !isLastStep,
        onSkip = ::finishOnboarding,
        onSwipeForward = ::goToNextStep,
        onSwipeBackward = ::goToPreviousStep,
        panel = {
            OnboardingChoicePanel(
                choice = currentStep.choice,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        bottomBar = {
            OnboardingFlowBottomBar(
                label = currentStep.buttonLabel,
                showBack = currentStepIndex > 0,
                onBack = ::goToPreviousStep,
                onNext = ::goToNextStep,
            )
        },
        showOnboardingMascot = true,
    )
}

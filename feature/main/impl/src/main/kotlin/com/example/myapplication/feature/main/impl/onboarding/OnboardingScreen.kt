package com.example.myapplication.feature.main.impl.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    val steps = viewModel.steps
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()

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
        if (isLastStep) finishOnboarding() else viewModel.goToNextStep()
    }

    fun goToPreviousStep() {
        viewModel.goToPreviousStep()
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
                label = stringResource(id = currentStep.buttonLabelRes),
                showBack = currentStepIndex > 0,
                onBack = ::goToPreviousStep,
                onNext = ::goToNextStep,
            )
        },
        showOnboardingMascot = true,
    )
}

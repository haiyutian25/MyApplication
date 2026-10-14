package com.example.myapplication.feature.main.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.platform.base.util.EventsEffect
import com.example.myapplication.ui.platform.components.ImmersiveTopBar
import com.example.myapplication.ui.platform.components.ImmersiveTopBarIconButton
import com.example.myapplication.ui.platform.components.immersiveTopBarContentPadding

/**
 * The home screen of the application, demonstrating the standard ViewModel / Screen pairing.
 *
 * The screen uses an immersive top layout: the content extends all the way to the top of the
 * screen, and the settings entry point floats independently above it.
 */
@Composable
fun HomeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettingsScreen: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            HomeEvent.NavigateBack -> onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = immersiveTopBarContentPadding())
                .navigationBarsPadding()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(id = R.string.home_count, state.count))
            Button(
                onClick = { viewModel.trySendAction(HomeAction.IncrementClick) },
            ) {
                Text(text = stringResource(id = R.string.home_increment))
            }
        }

        ImmersiveTopBar(
            trailingContent = {
                ImmersiveTopBarIconButton(onClick = onNavigateToSettingsScreen) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.home_open_settings),
                    )
                }
            },
        )
    }
}

package com.example.myapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.feature.settings.impl.ParticleLaunchReel
import com.example.myapplication.ui.platform.navigation.MyApplicationNavDisplay
import com.example.myapplication.ui.platform.theme.ThemeSwitchHost
import com.example.myapplication.ui.platform.util.isDarkMode
import dagger.hilt.android.AndroidEntryPoint

/**
 * The main [AppCompatActivity] for the app, responsible for hosting the Compose hierarchy.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()

            // The app opens with the reel, on every launch: the particles gather into the name, the
            // word lands, and the app is shown under it. The state survives a configuration change,
            // so turning the phone over does not play it a second time.
            var isLaunching by rememberSaveable { mutableStateOf(true) }

            ThemeSwitchHost(isDarkMode = state.appTheme.isDarkMode()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MyApplicationNavDisplay(isFirstLaunch = state.isFirstLaunch)

                    if (isLaunching) {
                        ParticleLaunchReel(onFinished = { isLaunching = false })
                    }
                }
            }
        }
    }
}

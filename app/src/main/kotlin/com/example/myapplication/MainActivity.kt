package com.example.myapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

            ThemeSwitchHost(isDarkMode = state.appTheme.isDarkMode()) {
                MyApplicationNavDisplay(isFirstLaunch = state.isFirstLaunch)
            }
        }
    }
}

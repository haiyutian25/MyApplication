package com.example.myapplication.feature.settings.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.model.AppLanguage
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.repository.applanguage.AppLanguageRepository
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import com.example.myapplication.network.AppUpdateManager
import com.example.myapplication.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages the state and behavior of the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLanguageRepository: AppLanguageRepository,
    private val appUpdateManager: AppUpdateManager,
) : BaseViewModel<SettingsState, Unit, SettingsAction>(
    initialState = SettingsState(
        appTheme = userPreferencesRepository.appTheme,
        appLanguage = appLanguageRepository.appLanguageFlow.value,
        appUpdate = AppUpdateUiState(),
    ),
) {
    init {
        userPreferencesRepository
            .appThemeFlow
            .onEach { appTheme ->
                sendAction(SettingsAction.Internal.AppThemeUpdateReceive(appTheme = appTheme))
            }
            .launchIn(scope = viewModelScope)

        appLanguageRepository
            .appLanguageFlow
            .onEach { appLanguage ->
                sendAction(SettingsAction.Internal.AppLanguageUpdateReceive(appLanguage))
            }
            .launchIn(scope = viewModelScope)

        mutableStateFlow.update {
            it.copy(appUpdate = it.appUpdate.copy(currentVersion = currentVersionName()))
        }
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.AppThemeSelect -> handleAppThemeSelect(action)
            is SettingsAction.AppLanguageSelect -> handleAppLanguageSelect(action)
            is SettingsAction.CheckForUpdates -> handleCheckForUpdates()
            is SettingsAction.DownloadAndInstallUpdate -> handleDownloadAndInstallUpdate()
            is SettingsAction.OpenReleasePage -> handleOpenReleasePage()
            is SettingsAction.DismissUpdate -> handleDismissUpdate()
            is SettingsAction.Internal.AppLanguageUpdateReceive -> {
                handleAppLanguageUpdateReceive(action)
            }
            is SettingsAction.Internal.AppThemeUpdateReceive -> {
                handleAppThemeUpdateReceive(action)
            }
        }
    }

    private fun handleAppThemeSelect(action: SettingsAction.AppThemeSelect) {
        userPreferencesRepository.setAppTheme(appTheme = action.appTheme)
    }

    private fun handleAppLanguageSelect(action: SettingsAction.AppLanguageSelect) {
        appLanguageRepository.setAppLanguage(appLanguage = action.appLanguage)
    }

    private fun handleAppLanguageUpdateReceive(
        action: SettingsAction.Internal.AppLanguageUpdateReceive,
    ) {
        mutableStateFlow.update { it.copy(appLanguage = action.appLanguage) }
    }

    private fun handleAppThemeUpdateReceive(
        action: SettingsAction.Internal.AppThemeUpdateReceive,
    ) {
        mutableStateFlow.update { it.copy(appTheme = action.appTheme) }
    }

    private fun handleCheckForUpdates() {
        val update = mutableStateFlow.value.appUpdate
        if (update.isChecking || update.isDownloading) return

        viewModelScope.launch {
            mutableStateFlow.update {
                it.copy(appUpdate = it.appUpdate.copy(isChecking = true, errorMessage = null))
            }
            appUpdateManager.checkUpdate(currentVersionName())
                .onSuccess { info ->
                    mutableStateFlow.update {
                        it.copy(appUpdate = it.appUpdate.copy(isChecking = false, info = info))
                    }
                }
                .onFailure { error ->
                    mutableStateFlow.update {
                        it.copy(
                            appUpdate = it.appUpdate.copy(
                                isChecking = false,
                                errorMessage = error.message,
                            ),
                        )
                    }
                }
        }
    }

    private fun handleDownloadAndInstallUpdate() {
        val url = mutableStateFlow.value.appUpdate.info?.apkDownloadUrl ?: return
        if (mutableStateFlow.value.appUpdate.isDownloading) return

        viewModelScope.launch {
            mutableStateFlow.update {
                it.copy(
                    appUpdate = it.appUpdate.copy(
                        isDownloading = true,
                        downloadProgress = 0f,
                        errorMessage = null,
                    ),
                )
            }
            appUpdateManager.downloadApk(url) { downloaded, total ->
                mutableStateFlow.update {
                    it.copy(
                        appUpdate = it.appUpdate.copy(
                            downloadProgress = if (total != null && total > 0) {
                                downloaded.toFloat() / total.toFloat()
                            } else {
                                null
                            },
                        ),
                    )
                }
            }
                .onSuccess { apkFile ->
                    mutableStateFlow.update {
                        it.copy(
                            appUpdate = it.appUpdate.copy(
                                isDownloading = false,
                                downloadProgress = 1f,
                            ),
                        )
                    }
                    appUpdateManager.installApk(apkFile)
                }
                .onFailure { error ->
                    mutableStateFlow.update {
                        it.copy(
                            appUpdate = it.appUpdate.copy(
                                isDownloading = false,
                                downloadProgress = null,
                                errorMessage = error.message,
                            ),
                        )
                    }
                }
        }
    }

    private fun handleOpenReleasePage() {
        val url = mutableStateFlow.value.appUpdate.info?.releaseUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun handleDismissUpdate() {
        mutableStateFlow.update {
            it.copy(appUpdate = AppUpdateUiState(currentVersion = currentVersionName()))
        }
    }

    /**
     * The version name of the installed app, as shown in the manifest.
     */
    private fun currentVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
}

/**
 * Models the state of the settings screen.
 */
@Parcelize
data class SettingsState(
    val appTheme: AppTheme,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
) : Parcelable

/**
 * Models actions that the settings screen may receive.
 */
sealed class SettingsAction {
    /**
     * The user has selected a new theme.
     */
    data class AppThemeSelect(val appTheme: AppTheme) : SettingsAction()

    /**
     * The user has picked the language the app should be shown in.
     */
    data class AppLanguageSelect(val appLanguage: AppLanguage) : SettingsAction()

    /**
     * The user has asked whether a newer build of the app has been published.
     */
    data object CheckForUpdates : SettingsAction()

    /**
     * The user has asked for the newer build the last check found to be downloaded and installed.
     */
    data object DownloadAndInstallUpdate : SettingsAction()

    /**
     * The user has asked for the page of the latest release to be opened in a browser.
     */
    data object OpenReleasePage : SettingsAction()

    /**
     * The dialog the last check opened has been closed.
     */
    data object DismissUpdate : SettingsAction()

    /**
     * Models internal actions that come from asynchronous processes within the ViewModel.
     */
    sealed class Internal : SettingsAction() {
        /**
         * The language has been updated from the data layer.
         */
        data class AppLanguageUpdateReceive(val appLanguage: AppLanguage) : Internal()
        /**
         * The app theme has been updated from the data layer.
         */
        data class AppThemeUpdateReceive(val appTheme: AppTheme) : Internal()
    }
}

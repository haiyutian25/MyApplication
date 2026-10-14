package com.example.myapplication.feature.settings.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.network.AppUpdateInfo
import com.example.myapplication.network.AppUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The state of the "check for updates" entry of the settings screen.
 */
data class AppUpdateUiState(
    val currentVersion: String = "",
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val info: AppUpdateInfo? = null,
    val errorMessage: String? = null,
)

/**
 * Drives the "check for updates" entry of the settings screen: it asks [AppUpdateManager] whether a
 * newer build of the app has been published on GitHub Releases and, when there is one, downloads it
 * and hands it over to the system installer.
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppUpdateUiState(currentVersion = currentVersionName()),
    )
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    /**
     * Closes the dialog the last check opened. The entry keeps showing the version of the app.
     */
    fun dismissUpdate() {
        _state.value = AppUpdateUiState(currentVersion = currentVersionName())
    }

    /**
     * Opens the page of the latest release in a browser, for the release notes that the dialog
     * leaves out.
     */
    fun openReleasePage() {
        val url = _state.value.info?.releaseUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Asks GitHub Releases for the latest published build and compares it with the version of the
     * installed app.
     */
    fun checkForUpdates() {
        if (_state.value.isChecking || _state.value.isDownloading) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isChecking = true, errorMessage = null)
            appUpdateManager.checkUpdate(currentVersionName())
                .onSuccess { info ->
                    _state.value = _state.value.copy(isChecking = false, info = info)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isChecking = false,
                        errorMessage = error.message ?: "检查更新失败",
                    )
                }
        }
    }

    /**
     * Downloads the newer build of the latest checked release and installs it.
     */
    fun downloadAndInstall() {
        val url = _state.value.info?.apkDownloadUrl ?: return
        if (_state.value.isDownloading) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                errorMessage = null,
            )
            appUpdateManager.downloadApk(url) { downloaded, total ->
                _state.value = _state.value.copy(
                    downloadProgress = if (total != null && total > 0) {
                        downloaded.toFloat() / total.toFloat()
                    } else {
                        null
                    },
                )
            }
                .onSuccess { apkFile ->
                    _state.value = _state.value.copy(isDownloading = false, downloadProgress = 1f)
                    appUpdateManager.installApk(apkFile)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        errorMessage = error.message ?: "下载更新包失败",
                    )
                }
        }
    }

    /**
     * The version name of the installed app, as shown in the manifest.
     */
    private fun currentVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
}

package com.example.myapplication.feature.settings.impl

import android.os.Parcelable
import com.example.myapplication.network.AppUpdateInfo
import kotlinx.parcelize.Parcelize

/**
 * The state of the "check for updates" entry of the settings screen: the version of the installed
 * app, whether a check or a download is running, and what the last check found.
 */
@Parcelize
data class AppUpdateUiState(
    val currentVersion: String = "",
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val info: AppUpdateInfo? = null,
    val errorMessage: String? = null,
) : Parcelable

// This file is an upstream copy and is kept as it is: the style rules of this project are waived
// for it so that it stays identical to the source it is copied from.
@file:Suppress("UndocumentedPublicClass")

package com.example.myapplication.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 太墟 · 版本更新信息模型 (GitHub Releases API)
 */
@Serializable
@Parcelize
data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val hasUpdate: Boolean,
    val releaseTitle: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkDownloadUrl: String? = null,
    val apkSizeBytes: Long? = null,
    val publishedAt: String = "",
) : Parcelable

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class Success(val info: AppUpdateInfo) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

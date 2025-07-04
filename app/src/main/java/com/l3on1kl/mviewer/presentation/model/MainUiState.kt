package com.l3on1kl.mviewer.presentation.model

import android.net.Uri

sealed interface MainUiState {
    object Idle : MainUiState
    object Loading : MainUiState

    data class Success(
        val doc: DocumentArgs,
        val uri: Uri?
    ) : MainUiState

    data class Error(val error: UiError) : MainUiState
}

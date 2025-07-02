package com.l3on1kl.mviewer.presentation.model

sealed interface DocumentViewerUiState {
    object Loading : DocumentViewerUiState
    data class Text(val text: String) : DocumentViewerUiState
    data class Error(val throwable: Throwable) : DocumentViewerUiState
}

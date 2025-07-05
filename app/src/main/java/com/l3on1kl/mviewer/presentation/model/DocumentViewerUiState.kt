package com.l3on1kl.mviewer.presentation.model

sealed interface DocumentViewerUiState {
    object Loading : DocumentViewerUiState

    data class Success(
        val items: List<MarkdownRenderItem>,
        val content: String
    ) : DocumentViewerUiState

    data class Error(val error: UiError) : DocumentViewerUiState
}

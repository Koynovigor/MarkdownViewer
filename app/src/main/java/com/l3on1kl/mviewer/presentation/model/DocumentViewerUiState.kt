package com.l3on1kl.mviewer.presentation.model

import com.l3on1kl.mviewer.domain.model.MarkdownRenderItem

sealed interface DocumentViewerUiState {
    object Loading : DocumentViewerUiState

    data class Success(
        val items: List<MarkdownRenderItem>,
        val content: String
    ) : DocumentViewerUiState

    data class Error(val error: UiError) : DocumentViewerUiState
}

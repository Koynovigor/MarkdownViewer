package com.l3on1kl.mviewer.presentation.model

import com.l3on1kl.mviewer.domain.model.MarkdownElement

sealed interface DocumentViewerUiState {
    object Loading : DocumentViewerUiState

    data class Success(val elements: List<MarkdownElement>) : DocumentViewerUiState

    data class Error(val error: UiError) : DocumentViewerUiState
}

package com.l3on1kl.mviewer.presentation.model

import android.net.Uri
import com.l3on1kl.mviewer.domain.model.MarkdownElement

sealed interface MainUiState {
    object Idle : MainUiState
    object Loading : MainUiState
    data class Success(
        val doc: DocumentArgs,
        val elements: List<MarkdownElement>,
        val uri: Uri?
    ) : MainUiState

    data class Error(val throwable: Throwable) : MainUiState
}

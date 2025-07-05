package com.l3on1kl.mviewer.presentation.model

sealed interface MainUiState {
    object None : MainUiState

    object Loading : MainUiState

    data class Error(val error: UiError) : MainUiState
}

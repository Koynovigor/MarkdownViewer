package com.l3on1kl.mviewer.presentation.model

import android.graphics.Bitmap

sealed interface DocumentViewerUiState {
    object Loading : DocumentViewerUiState
    data class Text(val text: String) : DocumentViewerUiState
    data class Pdf(val bitmap: Bitmap, val page: Int, val pageCount: Int) : DocumentViewerUiState
    data class Error(val throwable: Throwable) : DocumentViewerUiState
}

package com.l3on1kl.mviewer.presentation.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.usecase.LoadViewerDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel responsible for displaying a document. It loads text files through
 * [com.l3on1kl.mviewer.domain.usecase.LoadDocumentUseCase] and renders PDF pages using [android.graphics.pdf.PdfRenderer].
 */
@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val loadViewer: LoadViewerDocumentUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    sealed interface UiState {
        object Loading : UiState
        data class Text(val text: String) : UiState
        data class Pdf(val bitmap: Bitmap, val page: Int, val pageCount: Int) : UiState
        data class Error(val throwable: Throwable) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage = 0

    fun load(document: MarkdownDocument) {
        viewModelScope.launch {
            _uiState.value = UiState.Text(document.content)
        }
    }

    private suspend fun loadText(uri: Uri) {
        val result = loadViewer(uri)
        when (result) {
            is LoadViewerDocumentUseCase.Result.Text -> _uiState.value = UiState.Text(result.text)
            is LoadViewerDocumentUseCase.Result.Pdf -> openPdf(result.uri)
        }
    }

    private suspend fun openPdf(uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                pdfRenderer = PdfRenderer(fileDescriptor!!)
            }
            renderPage(0)
        } catch (t: Throwable) {
            _uiState.value = UiState.Error(t)
        }
    }

    /** Shows the next page if possible. */
    fun nextPage() {
        val renderer = pdfRenderer ?: return
        if (currentPage < renderer.pageCount - 1) {
            renderPage(currentPage + 1)
        }
    }

    /** Shows the previous page if possible. */
    fun prevPage() {
        if (currentPage > 0) {
            renderPage(currentPage - 1)
        }
    }

    private fun renderPage(index: Int) {
        val renderer = pdfRenderer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val page = renderer.openPage(index)
            val bitmap = createBitmap(page.width, page.height)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            currentPage = index
            _uiState.value = UiState.Pdf(bitmap, index + 1, renderer.pageCount)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

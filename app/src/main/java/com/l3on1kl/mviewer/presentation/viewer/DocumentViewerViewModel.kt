package com.l3on1kl.mviewer.presentation.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.usecase.SaveDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for displaying a document. It loads text files through
 * [com.l3on1kl.mviewer.domain.usecase.LoadDocumentUseCase] and renders PDF pages using [android.graphics.pdf.PdfRenderer].
 */
@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val saveDoc: SaveDocumentUseCase
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
    private var document: MarkdownDocument? = null

    fun load(document: MarkdownDocument) {
        this.document = document
        viewModelScope.launch {
            _uiState.value = UiState.Text(document.content)
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

    suspend fun saveDocument(content: String, uri: Uri? = null): Result<Unit> {
        val current =
            document ?: return Result.failure(IllegalStateException("Document not loaded"))
        val updated = current.copy(content = content, path = uri?.toString() ?: current.path)
        val result = saveDoc(updated)
        if (result.isSuccess) {
            document = updated
            _uiState.value = UiState.Text(content)
        }
        return result
    }
}

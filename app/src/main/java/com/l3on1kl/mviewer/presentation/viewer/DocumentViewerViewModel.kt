package com.l3on1kl.mviewer.presentation.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.usecase.ParseMarkdownUseCase
import com.l3on1kl.mviewer.domain.usecase.SaveDocumentUseCase
import com.l3on1kl.mviewer.presentation.model.DocumentViewerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val saveDoc: SaveDocumentUseCase,
    private val parseMd: ParseMarkdownUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentViewerUiState>(DocumentViewerUiState.Loading)
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    private var document: MarkdownDocument? = null

    fun load(document: MarkdownDocument) {
        this.document = document
        viewModelScope.launch {
            val elements = parseMd(document.content)
            _uiState.value = DocumentViewerUiState.Success(elements)
        }
    }

    suspend fun saveDocument(content: String, uri: Uri? = null): Result<Unit> {
        val current =
            document ?: return Result.failure(IllegalStateException("Document not loaded"))
        val updated = current.copy(content = content, path = uri?.toString() ?: current.path)

        return saveDoc(updated).onSuccess {
            document = updated
            val elements = parseMd(content)
            _uiState.value = DocumentViewerUiState.Success(elements)
        }.onFailure {
            _uiState.value = DocumentViewerUiState.Error(
                com.l3on1kl.mviewer.presentation.model.UiError.Unexpected(it)
            )
        }
    }
}

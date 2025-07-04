package com.l3on1kl.mviewer.presentation.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.usecase.SaveDocumentUseCase
import com.l3on1kl.mviewer.presentation.model.DocumentViewerUiState
import com.l3on1kl.mviewer.presentation.model.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val saveDoc: SaveDocumentUseCase,
    private val renderer: MarkdownRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentViewerUiState>(DocumentViewerUiState.Loading)
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    private var document: MarkdownDocument? = null

    fun load(doc: MarkdownDocument) {
        document = doc
        viewModelScope.launch {
            _uiState.value = DocumentViewerUiState.Loading
            val items = withContext(Dispatchers.Default) { renderer.render(doc) }
            _uiState.value = DocumentViewerUiState.Success(items, doc.content)
        }
    }


    suspend fun saveDocument(content: String, uri: Uri? = null): Result<Unit> {
        val current =
            document ?: return Result.failure(IllegalStateException("Document not loaded"))
        val updated = current.copy(content = content, path = uri?.toString() ?: current.path)

        return saveDoc(updated).onSuccess {
            document = updated
            val items = renderer.render(updated)
            _uiState.value = DocumentViewerUiState.Success(items, updated.content)
        }.onFailure {
            _uiState.value = DocumentViewerUiState.Error(UiError.Unexpected(it))
        }
    }
}

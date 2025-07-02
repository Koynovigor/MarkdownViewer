package com.l3on1kl.mviewer.presentation.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import com.l3on1kl.mviewer.domain.usecase.LoadDocumentUseCase
import com.l3on1kl.mviewer.domain.usecase.ParseMarkdownUseCase
import com.l3on1kl.mviewer.domain.usecase.SaveDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import javax.inject.Inject

/**
 * View model used by the document loading screen. It simply delegates to
 * domain use cases to verify that data flows through the layers.
 */
/* presentation/main/MainViewModel.kt */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadDoc: LoadDocumentUseCase,
    private val saveDoc: SaveDocumentUseCase,
    private val parseMd: ParseMarkdownUseCase
) : ViewModel() {

    sealed interface UiState {
        object Idle : UiState
        object Loading : UiState
        data class Success(val doc: MarkdownDocument, val elements: List<MarkdownElement>) : UiState
        data class Error(val throwable: Throwable) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Вызывает загрузку локального файла (SAF) */
    fun onLocalFileSelected(uri: Uri) = viewModelScope.launch {
        _uiState.value = UiState.Loading
        val result = loadDoc(LoadRequest.Local(File(uri.path!!)))
        handleResult(result)
    }

    /** Вызывает загрузку по URL */
    fun onUrlEntered(url: String) = viewModelScope.launch {
        _uiState.value = UiState.Loading
        val result = loadDoc(LoadRequest.Remote(URL(url)))
        handleResult(result)
    }

    private suspend fun handleResult(result: Result<MarkdownDocument>) {
        _uiState.value = result.fold(
            onSuccess = { doc ->
                val elements = parseMd(doc.content)
                UiState.Success(doc, elements)
            },
            onFailure = { UiState.Error(it) }
        )
    }

    /** Сохранение правок */
    fun onSave(doc: MarkdownDocument) = viewModelScope.launch {
        saveDoc(doc)
    }
}

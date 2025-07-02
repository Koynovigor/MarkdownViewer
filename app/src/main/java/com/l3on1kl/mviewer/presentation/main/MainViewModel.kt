package com.l3on1kl.mviewer.presentation.main

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
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
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

/**
 * View model used by the document loading screen. It simply delegates to
 * domain use cases to verify that data flows through the layers.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadDoc: LoadDocumentUseCase,
    private val saveDoc: SaveDocumentUseCase,
    private val parseMd: ParseMarkdownUseCase
) : ViewModel() {

    sealed interface UiState {
        object Idle : UiState
        object Loading : UiState
        data class Success(
            val doc: MarkdownDocument,
            val elements: List<MarkdownElement>,
            val uri: Uri?
        ) : UiState

        data class Error(val throwable: Throwable) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Вызывает загрузку локального файла (SAF) */
    fun onLocalFileSelected(uri: Uri) = viewModelScope.launch {
        _uiState.value = UiState.Loading
        val result = loadDoc(LoadRequest.Local(uri))
        handleResult(result, uri)
    }

    /** Вызывает загрузку по URL */
    fun onUrlEntered(url: String) = viewModelScope.launch {
        try {
            _uiState.value = UiState.Loading

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw MalformedURLException("URL must start with http:// or https://")
            }

            val result = loadDoc(LoadRequest.Remote(URL(url)))
            handleResult(result, url.toUri())

        } catch (e: Exception) {
            Log.e("MainViewModel", "Invalid URL: $url", e)
            _uiState.value = UiState.Error(e)
        }
    }

    private fun handleResult(result: Result<MarkdownDocument>, uri: Uri? = null) {
        _uiState.value = result.fold(
            onSuccess = { doc ->
                val elements = parseMd(doc.content)
                UiState.Success(doc, elements, uri)
            },
            onFailure = {
                Log.e("MainViewModel", "Error loading document", it)
                UiState.Error(it)
            }
        )
    }

    fun resetToIdle() {
        _uiState.value = UiState.Idle
    }

    /** Сохранение правок */
    fun onSave(doc: MarkdownDocument) = viewModelScope.launch {
        saveDoc(doc)
    }
}

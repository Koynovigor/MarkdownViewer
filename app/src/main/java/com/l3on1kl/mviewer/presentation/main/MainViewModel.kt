package com.l3on1kl.mviewer.presentation.main

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import com.l3on1kl.mviewer.domain.usecase.LoadDocumentUseCase
import com.l3on1kl.mviewer.domain.usecase.ParseMarkdownUseCase
import com.l3on1kl.mviewer.presentation.model.MainUiState
import com.l3on1kl.mviewer.presentation.model.toArgs
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
    private val parseMd: ParseMarkdownUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Вызывает загрузку локального файла (SAF) */
    fun onLocalFileSelected(uri: Uri) = viewModelScope.launch {
        _uiState.value = MainUiState.Loading
        val result = loadDoc(LoadRequest.Local(uri.toString()))
        handleResult(result, uri)
    }

    /** Вызывает загрузку по URL */
    fun onUrlEntered(url: String) = viewModelScope.launch {
        try {
            _uiState.value = MainUiState.Loading

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw MalformedURLException("URL must start with http:// or https://")
            }

            val result = loadDoc(LoadRequest.Remote(URL(url)))
            handleResult(result, url.toUri())

        } catch (e: Exception) {
            Log.e("MainViewModel", "Invalid URL: $url", e)
            _uiState.value = MainUiState.Error(e)
        }
    }

    private fun handleResult(result: Result<MarkdownDocument>, uri: Uri? = null) {
        _uiState.value = result.fold(
            onSuccess = { doc ->
                val elements = parseMd(doc.content)
                MainUiState.Success(doc.toArgs(), elements, uri)
            },
            onFailure = {
                Log.e("MainViewModel", "Error loading document", it)
                MainUiState.Error(it)
            }
        )
    }

    fun resetToIdle() {
        _uiState.value = MainUiState.Idle
    }
}

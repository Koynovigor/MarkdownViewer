package com.l3on1kl.mviewer.presentation.main

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import com.l3on1kl.mviewer.domain.usecase.LoadDocumentUseCase
import com.l3on1kl.mviewer.presentation.model.MainUiState
import com.l3on1kl.mviewer.presentation.model.UiError
import com.l3on1kl.mviewer.presentation.model.toArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadDoc: LoadDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onLocalFileSelected(uri: Uri) = viewModelScope.launch {
        _uiState.value = MainUiState.Loading
        val result = loadDoc(LoadRequest.Local(uri.toString()))
        handleResult(result, uri)
    }


    fun onUrlEntered(url: String) = viewModelScope.launch {
        _uiState.value = MainUiState.Loading

        runCatching {
            require(url.startsWith("http://") || url.startsWith("https://")) {
                "URL must start with http:// or https://"
            }
            loadDoc(LoadRequest.Remote(URL(url)))
        }.onSuccess { result ->
            handleResult(result, url.toUri())
        }.onFailure {
            _uiState.value = MainUiState.Error(UiError.InvalidUrl)
        }
    }

    fun resetToIdle() {
        _uiState.value = MainUiState.Idle
    }


    private fun handleResult(result: Result<MarkdownDocument>, uri: Uri?) {
        _uiState.value = result.fold(
            onSuccess = { doc -> MainUiState.Success(doc.toArgs(), uri) },
            onFailure = { e -> MainUiState.Error(UiError.Unexpected(e)) }
        )
    }
}

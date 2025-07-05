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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadDoc: LoadDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.None)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainNavEvent>()
    val events: SharedFlow<MainNavEvent> = _events.asSharedFlow()

    fun onLocalFileSelected(uri: Uri) = viewModelScope.launch {
        _uiState.value = MainUiState.Loading
        emitResult(
            loadDoc(
                LoadRequest.Local(uri.toString())
            ),
            uri
        )
    }

    fun onUrlEntered(url: String) = viewModelScope.launch {
        _uiState.value = MainUiState.Loading
        runCatching {
            require(
                url.startsWith("http://") || url.startsWith("https://")
            ) {
                "URL must start with http:// or https://"
            }
            loadDoc(
                LoadRequest.Remote(URL(url))
            )
        }
            .onSuccess { result ->
                emitResult(result, url.toUri())
            }
            .onFailure {
                _uiState.value = MainUiState.Error(UiError.InvalidUrl)
            }
    }

    private fun Throwable.toUiError(): UiError = when (this) {
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.SocketTimeoutException -> UiError.NoInternet

        else -> UiError.Unexpected(this)
    }

    private suspend fun emitResult(result: Result<MarkdownDocument>, uri: Uri?) {
        result.fold(
            onSuccess = {
                _events.emit(
                    MainNavEvent.OpenDocument(it.toArgs(), uri)
                )
            },
            onFailure = {
                _uiState.value = MainUiState.Error(it.toUiError())
            }
        )
        _uiState.value = MainUiState.None
    }
}

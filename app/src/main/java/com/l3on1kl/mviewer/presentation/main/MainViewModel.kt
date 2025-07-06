package com.l3on1kl.mviewer.presentation.main

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l3on1kl.mviewer.domain.model.HistoryEntry
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import com.l3on1kl.mviewer.domain.usecase.AddToHistoryUseCase
import com.l3on1kl.mviewer.domain.usecase.GetHistoryUseCase
import com.l3on1kl.mviewer.domain.usecase.LoadDocumentUseCase
import com.l3on1kl.mviewer.domain.usecase.RemoveFromHistoryUseCase
import com.l3on1kl.mviewer.presentation.model.MainUiState
import com.l3on1kl.mviewer.presentation.model.UiError
import com.l3on1kl.mviewer.presentation.model.toArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadDoc: LoadDocumentUseCase,
    private val addToHistory: AddToHistoryUseCase,
    private val removeFromHistory: RemoveFromHistoryUseCase,
    getHistory: GetHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.None)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainNavEvent>()
    val events: SharedFlow<MainNavEvent> = _events.asSharedFlow()

    val history = getHistory()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    fun onLocalFileSelected(
        uri: Uri,
        name: String
    ) {
        performLoad(
            request = LoadRequest.Local(uri.toString()),
            prettyName = name,
            originalUri = uri
        )
    }

    fun resetUiState() {
        _uiState.value = MainUiState.None
    }

    fun onUrlEntered(url: String) {
        performLoad(
            request = LoadRequest.Remote(
                URL(url)
            ),
            prettyName = url.substringAfterLast('/'),
            originalUri = url.toUri()
        )
    }

    private fun performLoad(
        request: LoadRequest,
        prettyName: String,
        originalUri: Uri
    ) = viewModelScope.launch {
        val delayJob = launch {
            delay(300)
            _uiState.value = MainUiState.Loading
        }

        loadDoc(request)
            .onSuccess { doc ->
                delayJob.cancel()

                _events.emit(
                    MainNavEvent.OpenDocument(
                        doc.toArgs(),
                        originalUri
                    )
                )

                launch {
                    addToHistory(
                        HistoryEntry(
                            path = originalUri.toString(),
                            name = prettyName,
                            openedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            .onFailure { error ->
                delayJob.cancel()
                _uiState.value = MainUiState.Error(error.toUiError())
            }
    }

    private fun Throwable.toUiError(): UiError = when (this) {
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.SocketTimeoutException -> UiError.NoInternet

        else -> UiError.Unexpected(this)
    }

    fun deleteHistory(path: String) = viewModelScope.launch {
        removeFromHistory(path)
    }
}

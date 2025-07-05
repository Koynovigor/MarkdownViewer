package com.l3on1kl.mviewer.presentation.main

import android.net.Uri
import com.l3on1kl.mviewer.presentation.model.DocumentArgs

sealed interface MainNavEvent {
    data class OpenDocument(val doc: DocumentArgs, val uri: Uri?) : MainNavEvent
}
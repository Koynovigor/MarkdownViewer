package com.l3on1kl.mviewer.presentation.model

import android.content.Context
import com.l3on1kl.mviewer.R

sealed interface UiError {
    fun getMessage(context: Context): String

    object FileNotFound : UiError {
        override fun getMessage(context: Context): String =
            context.getString(R.string.file_not_found)
    }

    object PermissionDenied : UiError {
        override fun getMessage(context: Context): String =
            context.getString(R.string.permission_denied)
    }

    object InvalidUrl : UiError {
        override fun getMessage(context: Context): String =
            context.getString(R.string.invalid_url)
    }

    data class Unexpected(val throwable: Throwable) : UiError {
        override fun getMessage(context: Context): String =
            throwable.localizedMessage?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.unexpected_error)
    }
}

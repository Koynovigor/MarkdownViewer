package com.l3on1kl.mviewer.presentation.model

import android.os.Parcelable
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import kotlinx.parcelize.Parcelize

@Parcelize
data class DocumentArgs(
    val id: String,
    val content: String,
    val path: String
) : Parcelable

fun MarkdownDocument.toArgs(): DocumentArgs =
    DocumentArgs(
        id,
        content,
        path
    )

fun DocumentArgs.toDomain(): MarkdownDocument =
    MarkdownDocument(
        id,
        content,
        path
    )

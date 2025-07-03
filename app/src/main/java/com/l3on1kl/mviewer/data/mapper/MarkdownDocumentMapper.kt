package com.l3on1kl.mviewer.data.mapper

import com.l3on1kl.mviewer.data.model.dto.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.model.MarkdownDocument

fun DataMarkdownDocument.toDomain(): MarkdownDocument =
    MarkdownDocument(id, content, path)

fun MarkdownDocument.toData(): DataMarkdownDocument =
    DataMarkdownDocument(id, content, path)

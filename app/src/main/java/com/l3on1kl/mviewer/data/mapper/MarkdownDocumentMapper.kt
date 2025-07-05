package com.l3on1kl.mviewer.data.mapper

import androidx.core.net.toUri
import com.l3on1kl.mviewer.data.model.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.model.MarkdownDocument

object MarkdownDocumentMapper {

    fun DataMarkdownDocument.toDomain(): MarkdownDocument =
        MarkdownDocument(
            id = id,
            content = content,
            path = path.toUri().let { uri ->
                if (uri.scheme == null || uri.scheme == "file")
                    uri.path ?: path
                else
                    uri.toString()
            }
        )

    fun MarkdownDocument.toData(): DataMarkdownDocument =
        DataMarkdownDocument(
            id = id,
            content = content,
            path = path
        )
}

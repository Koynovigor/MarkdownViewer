package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import java.io.File
import java.util.UUID
import javax.inject.Inject

class LocalFileDataSource @Inject constructor() : DocumentDataSource {

    override suspend fun canHandle(request: LoadRequest) =
        request is LoadRequest.Local

    override suspend fun load(request: LoadRequest): MarkdownDocument {
        request as LoadRequest.Local
        val file = request.file
        val content = file.readText()
        return MarkdownDocument(
            id = file.nameWithoutExtension.ifEmpty { UUID.randomUUID().toString() },
            content = content,
            path = file.absolutePath
        )
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> = runCatching {
        val dest = File(document.path ?: "${document.id}.md")
        dest.writeText(document.content)
    }
}

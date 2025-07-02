package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import java.net.HttpURLConnection
import java.util.UUID
import javax.inject.Inject

class RemoteUrlDataSource @Inject constructor() : DocumentDataSource {

    override suspend fun canHandle(request: LoadRequest) =
        request is LoadRequest.Remote

    override suspend fun load(request: LoadRequest): MarkdownDocument {
        request as LoadRequest.Remote
        val connection = (request.url.openConnection() as HttpURLConnection)
        val content = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        return MarkdownDocument(
            id = request.url.path.substringAfterLast('/').substringBeforeLast('.')
                .ifEmpty { UUID.randomUUID().toString() },
            content = content,
            path = request.url.toString()
        )
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> =
        Result.failure(UnsupportedOperationException("Remote write not supported"))
}

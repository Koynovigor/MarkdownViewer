package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class RemoteUrlDataSource @Inject constructor() : DocumentDataSource {

    override suspend fun canHandle(request: LoadRequest) =
        request is LoadRequest.Remote

    override suspend fun load(request: LoadRequest): MarkdownDocument {
        val url = (request as LoadRequest.Remote).url

        val content = withContext(Dispatchers.IO) {
            url.openStream().bufferedReader().use { it.readText() }
        }

        return MarkdownDocument(
            id = UUID.randomUUID().toString(),
            content = content,
            path = url.toString()
        )
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> =
        Result.failure(UnsupportedOperationException("Remote write not supported"))
}

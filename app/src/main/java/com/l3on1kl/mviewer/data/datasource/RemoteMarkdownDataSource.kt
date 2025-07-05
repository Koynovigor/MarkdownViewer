package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.data.model.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.util.UUID
import javax.inject.Inject

@Remote
class RemoteMarkdownDataSource @Inject constructor() : ReadableDocumentDataSource {

    override suspend fun load(request: LoadRequest): Result<DataMarkdownDocument> = runCatching {
        val remote = request as? LoadRequest.Remote
            ?: error("RemoteMarkdownDataSource can handle only Remote requests")

        withContext(Dispatchers.IO) {
            (remote.url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }.inputStream.bufferedReader().use { stream ->
                val text = stream.readText()
                DataMarkdownDocument(
                    id = UUID.randomUUID().toString(),
                    content = text,
                    path = remote.url.toString()
                )
            }
        }
    }
}

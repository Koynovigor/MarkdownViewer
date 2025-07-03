package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.data.model.dto.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class RemoteMarkdownDataSource @Inject constructor() : DocumentDataSource {

    override fun canHandle(request: LoadRequest): Boolean =
        request is LoadRequest.Remote

    override suspend fun load(request: LoadRequest): DataMarkdownDocument {
        require(request is LoadRequest.Remote) { "Unsupported request type: ${request::class}" }

        val url = request.url

        val content = try {
            withContext(Dispatchers.IO) {
                url.openStream().bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) {
            throw IOException("Unable to load remote markdown file: $url", e)
        }

        return DataMarkdownDocument(
            id = UUID.randomUUID().toString(),
            content = content,
            path = url.toString()
        )
    }

    override suspend fun save(document: DataMarkdownDocument): Boolean {
        throw UnsupportedOperationException("Saving to remote URL is not supported.")
    }
}

package com.l3on1kl.mviewer.data.datasource

import android.content.ContentResolver
import androidx.core.net.toUri
import com.l3on1kl.mviewer.data.model.dto.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class LocalMarkdownFileDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) : DocumentDataSource {

    override fun canHandle(request: LoadRequest): Boolean =
        request is LoadRequest.Local

    override suspend fun load(request: LoadRequest): DataMarkdownDocument {
        require(request is LoadRequest.Local) { "Unsupported request type: ${request::class}" }

        val uri = request.path.toUri()
        val content = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
        } ?: throw IllegalArgumentException("Cannot read file: $uri")

        val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""

        return DataMarkdownDocument(
            id = name.ifBlank { UUID.randomUUID().toString() },
            content = content,
            path = uri.toString()
        )
    }

    override suspend fun save(document: DataMarkdownDocument): Boolean {
        val uri = runCatching { document.path.toUri() }.getOrElse { throw it }

        return withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri, "w")
                ?.bufferedWriter()
                ?.use { it.write(document.content) }
                ?: throw IllegalArgumentException("Cannot open file for writing: $uri")
        }.let { true }
    }
}

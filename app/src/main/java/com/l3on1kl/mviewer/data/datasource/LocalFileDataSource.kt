package com.l3on1kl.mviewer.data.datasource

import android.content.Context
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class LocalFileDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DocumentDataSource {

    override suspend fun canHandle(request: LoadRequest): Boolean =
        request is LoadRequest.Local

    override suspend fun load(request: LoadRequest): MarkdownDocument {
        val uri = (request as LoadRequest.Local).uri
        val content = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalArgumentException("Cannot read file: $uri")

        val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""

        return MarkdownDocument(
            id = if (name.isNotBlank()) name else UUID.randomUUID().toString(),
            content = content,
            path = uri.toString()
        )
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> =
        Result.failure(UnsupportedOperationException("Save not supported for SAF URIs"))
}

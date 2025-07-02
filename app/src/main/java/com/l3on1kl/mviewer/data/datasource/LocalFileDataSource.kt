package com.l3on1kl.mviewer.data.datasource

import android.content.Context
import androidx.core.net.toUri
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class LocalFileDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DocumentDataSource {

    override suspend fun canHandle(request: LoadRequest): Boolean =
        request is LoadRequest.Local

    override suspend fun load(request: LoadRequest): MarkdownDocument {
        val uri = (request as LoadRequest.Local).path.toUri()
        val content = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
        } ?: throw IllegalArgumentException("Cannot read file: $uri")

        val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""

        return MarkdownDocument(
            id = name.ifBlank { UUID.randomUUID().toString() },
            content = content,
            path = uri.toString()
        )
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> {
        val uri = runCatching { document.path.toUri() }.getOrElse {
            return Result.failure(it)
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "w")
                    ?.bufferedWriter()
                    ?.use { it.write(document.content) }
                    ?: throw IllegalArgumentException("Cannot open file for writing: $uri")
            }
        }
    }
}

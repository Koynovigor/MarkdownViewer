package com.l3on1kl.mviewer.data.datasource

import android.content.ContentResolver
import androidx.core.net.toUri
import com.l3on1kl.mviewer.data.model.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

private const val OPEN_MODE_TRUNCATE = "rwt"

@Local
class LocalMarkdownFileDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) : WritableDocumentDataSource {

    override suspend fun load(request: LoadRequest): Result<DataMarkdownDocument> = runCatching {
        val local = request as? LoadRequest.Local
            ?: error("LocalMarkdownFileDataSource can handle only Local requests")

        val uri = local.path.toUri()

        val content = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } ?: throw DataSourceException.ReadFailed("Cannot read file: $uri")

        val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""
        DataMarkdownDocument(
            id = name.ifBlank { UUID.randomUUID().toString() },
            content = content,
            path = uri.toString()
        )
    }

    override suspend fun save(document: DataMarkdownDocument): Result<Unit> = runCatching {
        val uri = document.path.toUri()
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri, OPEN_MODE_TRUNCATE)
                ?.bufferedWriter()
                ?.use { it.write(document.content) }
                ?: throw DataSourceException.WriteFailed("Cannot open file for writing: $uri")
        }
    }
}

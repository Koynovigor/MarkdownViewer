package com.l3on1kl.mviewer.data.repository

import com.l3on1kl.mviewer.data.datasource.DocumentDataSource
import com.l3on1kl.mviewer.data.mapper.toData
import com.l3on1kl.mviewer.data.mapper.toDomain
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val dataSources: List<@JvmSuppressWildcards DocumentDataSource>
) : DocumentRepository {

    override suspend fun load(request: LoadRequest): MarkdownDocument {
        val source = dataSources.firstOrNull { it.canHandle(request) }
            ?: throw IllegalArgumentException("No data source for $request")
        return source.load(request).toDomain()
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> {
        val source = dataSources.firstOrNull { it.canHandle(LoadRequest.Local(document.path)) }
            ?: return Result.failure(IllegalStateException("No suitable data source to save: ${document.path}"))

        return runCatching {
            val success = source.save(document.toData())
            if (!success) throw IllegalStateException("Save operation failed")
        }
    }
}

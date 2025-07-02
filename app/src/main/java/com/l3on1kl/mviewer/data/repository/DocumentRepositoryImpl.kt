package com.l3on1kl.mviewer.data.repository

import com.l3on1kl.mviewer.data.datasource.DocumentDataSource
import com.l3on1kl.mviewer.data.datasource.LocalFileDataSource
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
        return source.load(request)
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> {
        val localSource = dataSources
            .firstOrNull { it is LocalFileDataSource }
            ?: return Result.failure(IllegalStateException("Local source absent"))
        return localSource.save(document)
    }
}

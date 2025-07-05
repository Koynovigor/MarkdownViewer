package com.l3on1kl.mviewer.data.repository

import com.l3on1kl.mviewer.data.datasource.Local
import com.l3on1kl.mviewer.data.datasource.ReadableDocumentDataSource
import com.l3on1kl.mviewer.data.datasource.Remote
import com.l3on1kl.mviewer.data.datasource.WritableDocumentDataSource
import com.l3on1kl.mviewer.data.mapper.MarkdownDocumentMapper.toData
import com.l3on1kl.mviewer.data.mapper.MarkdownDocumentMapper.toDomain
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    @param:Local private val local: WritableDocumentDataSource,
    @param:Remote private val remote: ReadableDocumentDataSource
) : DocumentRepository {

    override suspend fun load(request: LoadRequest) = when (request) {
        is LoadRequest.Local -> local.load(request).map { it.toDomain() }
        is LoadRequest.Remote -> remote.load(request).map { it.toDomain() }
    }

    override suspend fun save(document: MarkdownDocument): Result<Unit> =
        local.save(document.toData())
}

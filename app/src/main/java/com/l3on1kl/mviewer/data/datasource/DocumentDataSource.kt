package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest

interface DocumentDataSource {
    suspend fun canHandle(request: LoadRequest): Boolean
    suspend fun load(request: LoadRequest): MarkdownDocument
    suspend fun save(document: MarkdownDocument): Result<Unit>
}

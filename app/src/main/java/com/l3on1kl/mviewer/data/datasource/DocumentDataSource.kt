package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.data.model.dto.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest

interface ReadableDocumentDataSource {
    suspend fun load(request: LoadRequest): Result<DataMarkdownDocument>
}

interface WritableDocumentDataSource : ReadableDocumentDataSource {
    suspend fun save(document: DataMarkdownDocument): Result<Unit>
}

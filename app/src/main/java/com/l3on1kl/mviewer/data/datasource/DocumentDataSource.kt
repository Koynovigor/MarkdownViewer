package com.l3on1kl.mviewer.data.datasource

import com.l3on1kl.mviewer.data.model.dto.DataMarkdownDocument
import com.l3on1kl.mviewer.domain.repository.LoadRequest

interface DocumentDataSource {
    fun canHandle(request: LoadRequest): Boolean

    suspend fun load(request: LoadRequest): DataMarkdownDocument

    suspend fun save(document: DataMarkdownDocument): Boolean

}

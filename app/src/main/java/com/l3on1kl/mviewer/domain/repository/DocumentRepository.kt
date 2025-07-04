package com.l3on1kl.mviewer.domain.repository

import com.l3on1kl.mviewer.domain.model.MarkdownDocument

/**
 * Repository responsible for loading and saving Markdown documents.
 */
interface DocumentRepository {
    suspend fun load(request: LoadRequest): Result<MarkdownDocument>
    suspend fun save(document: MarkdownDocument): Result<Unit>
}

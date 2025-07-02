package com.l3on1kl.mviewer.domain

import com.l3on1kl.mviewer.domain.model.MarkdownDocument

/**
 * Repository responsible for loading and saving Markdown documents.
 */
interface DocumentRepository {
    fun load(path: String): MarkdownDocument
    fun save(document: MarkdownDocument)
}

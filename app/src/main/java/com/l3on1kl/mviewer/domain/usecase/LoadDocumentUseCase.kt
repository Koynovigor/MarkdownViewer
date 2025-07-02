package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.DocumentRepository
import com.l3on1kl.mviewer.domain.model.MarkdownDocument

/**
 * Loads a Markdown document from the repository.
 */
class LoadDocumentUseCase(private val repository: DocumentRepository) {
    operator fun invoke(path: String): MarkdownDocument = repository.load(path)
}

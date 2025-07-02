package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.DocumentRepository
import com.l3on1kl.mviewer.domain.model.MarkdownDocument

/**
 * Saves a Markdown document through the repository.
 */
class SaveDocumentUseCase(private val repository: DocumentRepository) {
    operator fun invoke(document: MarkdownDocument) {
        repository.save(document)
    }
}

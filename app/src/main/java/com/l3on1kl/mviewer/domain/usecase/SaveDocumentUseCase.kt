package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import javax.inject.Inject

/**
 * Saves a Markdown document through the repository.
 */
class SaveDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(
        document: MarkdownDocument
    ): Result<Unit> = repository.save(document)
}

package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import javax.inject.Inject

class SaveDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {

    suspend operator fun invoke(document: MarkdownDocument): Result<Unit> {
        if (document.path.isBlank()) {
            return Result.failure(InvalidDocumentException("Document path is blank"))
        }
        return repository.save(document)
    }

    class InvalidDocumentException(message: String) : IllegalArgumentException(message)
}

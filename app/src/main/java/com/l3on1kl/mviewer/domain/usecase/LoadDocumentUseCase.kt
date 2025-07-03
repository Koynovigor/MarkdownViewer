package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import javax.inject.Inject

class LoadDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {

    suspend operator fun invoke(request: LoadRequest): Result<MarkdownDocument> {
        return try {
            val doc = repository.load(request)
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

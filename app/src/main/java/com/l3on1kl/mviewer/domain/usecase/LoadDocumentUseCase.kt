package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import javax.inject.Inject

/**
 * Loads a Markdown document from the repository.
 */
class LoadDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    /**
     * @param request  – что именно надо загрузить
     * @return Result с MarkdownDocument внутри
     */
    suspend operator fun invoke(
        request: LoadRequest
    ): Result<MarkdownDocument> = runCatching {
        repository.load(request)
    }
}

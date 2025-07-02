package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import com.l3on1kl.mviewer.domain.repository.LoadRequest
import javax.inject.Inject

/**
 * Loads a document for the viewer. For text/markdown files the document content
 * is returned, for PDFs only the file path is propagated.
 */
class LoadViewerDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    sealed interface Result {
        data class Text(val text: String) : Result
        data class Pdf(val path: String) : Result
    }

    suspend operator fun invoke(path: String): Result =
        if (path.endsWith(".pdf", true)) {
            Result.Pdf(path)
        } else {
            val doc: MarkdownDocument = repository.load(
                LoadRequest.Local(path)
            )
            Result.Text(doc.content)
        }
}

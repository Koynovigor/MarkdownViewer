package com.l3on1kl.mviewer.domain.usecase

import android.net.Uri
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
        data class Pdf(val uri: Uri) : Result
    }

    suspend operator fun invoke(uri: Uri): Result =
        if (uri.toString().endsWith(".pdf", true)) {
            Result.Pdf(uri)
        } else {
            val doc: MarkdownDocument = repository.load(
                LoadRequest.Local(uri)
            )
            Result.Text(doc.content)
        }
}

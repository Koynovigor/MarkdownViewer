package com.l3on1kl.mviewer.domain.model

/**
 * Basic representation of a Markdown document.
 * @param id Unique identifier of the document.
 * @param content Raw markdown content.
 * @param path Path to local file or URL from which the document was loaded.
 */
data class MarkdownDocument(
    val id: String,
    val content: String,
    val path: String
)

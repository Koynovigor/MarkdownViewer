package com.l3on1kl.mviewer.domain.model

data class MarkdownDocument(
    val id: String,
    val content: String,
    val path: String
) {
    init {
        require(id.isNotBlank()) { "Document id must not be blank" }
        require(content.isNotBlank()) { "Document content must not be blank" }
        require(path.isNotBlank()) { "Document path must not be blank" }
    }
}

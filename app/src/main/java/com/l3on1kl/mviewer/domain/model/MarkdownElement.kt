package com.l3on1kl.mviewer.domain.model

enum class MarkdownElementType { Heading, Paragraph, Image, Quote, ListItem, Code }

data class MarkdownElement(
    val type: MarkdownElementType,
    val text: String,
    val params: Map<String, String> = emptyMap()
) {
    init {
        require(text.isNotBlank()) { "MarkdownElement.text cannot be blank" }
    }
}


package com.l3on1kl.mviewer.domain.model

enum class MarkdownElementType {
    Heading,
    Paragraph,
    Image,
    ListItem,
    Bold,
    Italic,
    Strikethrough,
    Table
}

data class MarkdownElement(
    val type: MarkdownElementType,
    val text: String,
    val params: Map<String, String> = emptyMap()
)

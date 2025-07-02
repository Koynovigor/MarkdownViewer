package com.l3on1kl.mviewer.domain.model

/**
 * Element of a parsed Markdown document.
 * @param type Type of the element (heading, image, text, etc.).
 * @param text Raw text associated with the element.
 * @param params Additional parameters like level for headings or url for images.
 */
data class MarkdownElement(
    val type: String,
    val text: String,
    val params: Map<String, String> = emptyMap()
)

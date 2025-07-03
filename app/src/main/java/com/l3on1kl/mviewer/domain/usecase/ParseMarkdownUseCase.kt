package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import javax.inject.Inject

class ParseMarkdownUseCase @Inject constructor() {
    operator fun invoke(content: String): List<MarkdownElement> {
        if (content.isBlank()) return emptyList()

        return content.lines()
            .filter { it.isNotBlank() }
            .map {
                val type = when {
                    it.startsWith("#") -> MarkdownElementType.Heading
                    it.startsWith(">") -> MarkdownElementType.Quote
                    it.startsWith("- ") || it.startsWith("* ") -> MarkdownElementType.ListItem
                    else -> MarkdownElementType.Paragraph
                }
                MarkdownElement(type = type, text = it.trim())
            }
    }
}

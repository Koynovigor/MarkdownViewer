package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownElement
import javax.inject.Inject

/**
 * Parses raw Markdown content into a list of elements.
 * Currently returns an empty list as a stub implementation.
 */
class ParseMarkdownUseCase @Inject constructor() {
    operator fun invoke(content: String): List<MarkdownElement> = emptyList()
}

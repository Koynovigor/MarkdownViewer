package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.parser.MarkdownParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParseMarkdownUseCase @Inject constructor(
    private val parser: MarkdownParser
) {
    operator fun invoke(markdown: String): List<MarkdownElement> =
        parser.parse(markdown)
}

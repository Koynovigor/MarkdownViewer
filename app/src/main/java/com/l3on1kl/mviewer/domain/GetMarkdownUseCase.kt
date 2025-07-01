package com.l3on1kl.mviewer.domain

class GetMarkdownUseCase(private val repository: MarkdownRepository) {
    operator fun invoke(): String = repository.getMarkdown()
}
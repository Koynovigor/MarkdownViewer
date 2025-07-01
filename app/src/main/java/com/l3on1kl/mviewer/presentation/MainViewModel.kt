package com.l3on1kl.mviewer.presentation

import com.l3on1kl.mviewer.domain.GetMarkdownUseCase

class MainViewModel(private val getMarkdownUseCase: GetMarkdownUseCase) {
    fun loadMarkdown(): String = getMarkdownUseCase()
}
package com.l3on1kl.mviewer.domain.parser

import com.l3on1kl.mviewer.domain.model.MarkdownElement

interface MarkdownParser {
    fun parse(content: String): List<MarkdownElement>
}

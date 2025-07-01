package com.l3on1kl.mviewer.data

import android.content.Context
import com.l3on1kl.mviewer.domain.MarkdownRepository

class AssetsMarkdownRepository(private val context: Context) : MarkdownRepository {
    override fun getMarkdown(): String {
        return context.assets.open("sample.md").bufferedReader().use { it.readText() }
    }
}
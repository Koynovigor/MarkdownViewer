package com.l3on1kl.mviewer.presentation.model

import android.text.Spanned

sealed class MarkdownRenderItem {
    data class Paragraph(val text: Spanned) : MarkdownRenderItem()

    data class Header(
        val text: CharSequence,
        val level: Int
    ) : MarkdownRenderItem()

    data class ListItem(
        val text: Spanned,
        val ordered: Boolean,
        val level: Int,
        val marker: String
    ) : MarkdownRenderItem()

    data class Image(
        val url: String,
        val alt: String?
    ) : MarkdownRenderItem()

    data class Table(val rows: List<List<Cell>>) : MarkdownRenderItem() {
        sealed interface Cell {
            data class Text(val text: CharSequence) : Cell
            data class Image(
                val url: String,
                val alt: String?
            ) : Cell
        }
    }

    object EmptyLine : MarkdownRenderItem()
}

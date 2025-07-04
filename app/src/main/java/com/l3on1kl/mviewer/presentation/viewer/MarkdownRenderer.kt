package com.l3on1kl.mviewer.presentation.viewer

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import com.l3on1kl.mviewer.domain.model.MarkdownRenderItem
import com.l3on1kl.mviewer.domain.parser.MarkdownParser
import javax.inject.Inject

class MarkdownRenderer @Inject constructor(
    private val parser: MarkdownParser
) {

    fun render(doc: MarkdownDocument): List<MarkdownRenderItem> =
        toRenderItems(parser.parse(doc.content))


    private fun toRenderItems(elements: List<MarkdownElement>): List<MarkdownRenderItem> {
        val out = mutableListOf<MarkdownRenderItem>()
        val paragraph = SpannableStringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                out += MarkdownRenderItem.Paragraph(SpannableStringBuilder(paragraph))
                paragraph.clear()
            }
        }

        var idx = 0
        while (idx < elements.size) {
            val e = elements[idx]

            when (e.type) {

                MarkdownElementType.Image -> {
                    flushParagraph()
                    out += MarkdownRenderItem.Image(
                        url = e.params["src"].orEmpty(),
                        alt = e.text.takeIf(String::isNotBlank)
                    )
                }

                MarkdownElementType.Heading -> {
                    flushParagraph()
                    out += MarkdownRenderItem.Header(
                        text = e.text,
                        level = e.params["level"]?.toIntOrNull() ?: 1
                    )
                }

                /* ─── Таблица ─── */
                MarkdownElementType.Table -> {
                    flushParagraph()

                    val rows = mutableListOf<List<MarkdownRenderItem.Table.Cell>>()

                    while (idx < elements.size && elements[idx].type == MarkdownElementType.Table) {
                        val rawCells = elements[idx].params["cells"]?.split(';')?.map { it.trim() }
                            ?: emptyList()

                        val parsedCells = rawCells.map { source ->
                            val inline = parser.parse(source)

                            if (inline.size == 1 && inline[0].type == MarkdownElementType.Image) {
                                val el = inline[0]
                                MarkdownRenderItem.Table.Cell.Image(
                                    url = el.params["src"].orEmpty(),
                                    alt = el.text.takeIf(String::isNotBlank)
                                )
                            } else {
                                MarkdownRenderItem.Table.Cell.Text(buildSpanFromInline(inline))
                            }
                        }
                        rows += parsedCells
                        idx++
                    }
                    out += MarkdownRenderItem.Table(rows)
                    continue
                }

                /* ─── Элементы списка ─── */
                MarkdownElementType.ListItem -> {
                    flushParagraph()
                    val liId = e.params["li"] ?: run { idx++; continue }

                    val span = SpannableStringBuilder()
                    var imageAfter: MarkdownRenderItem.Image? = null

                    while (idx < elements.size && elements[idx].params["li"] == liId) {
                        val cur = elements[idx]
                        val start = span.length
                        span.append(cur.text)
                        val end = span.length

                        when (cur.type) {
                            MarkdownElementType.Bold -> span.setSpan(
                                StyleSpan(Typeface.BOLD),
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            MarkdownElementType.Italic -> span.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            MarkdownElementType.Strikethrough -> span.setSpan(
                                StrikethroughSpan(),
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            MarkdownElementType.Image -> imageAfter = MarkdownRenderItem.Image(
                                url = cur.params["src"].orEmpty(),
                                alt = cur.text.takeIf(String::isNotBlank)
                            )

                            else -> {}
                        }
                        idx++
                    }

                    out += MarkdownRenderItem.ListItem(
                        text = span,
                        ordered = e.params["ordered"] == "true",
                        level = e.params["level"]?.toIntOrNull() ?: 0,
                        marker = e.params["marker"].orEmpty()
                    )
                    imageAfter?.let(out::add)
                    continue
                }

                /* ─── Inline-текст ─── */
                MarkdownElementType.Bold,
                MarkdownElementType.Italic,
                MarkdownElementType.Strikethrough,
                MarkdownElementType.Paragraph -> {
                    when {
                        e.text.isEmpty() -> {
                            flushParagraph()
                            out += MarkdownRenderItem.EmptyLine
                        }

                        e.text == "\n" -> {
                            paragraph.append('\n')
                        }

                        else -> {
                            val start = paragraph.length
                            paragraph.append(e.text)
                            val end = paragraph.length
                            when (e.type) {
                                MarkdownElementType.Bold -> paragraph.setSpan(
                                    StyleSpan(Typeface.BOLD),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                MarkdownElementType.Italic -> paragraph.setSpan(
                                    StyleSpan(Typeface.ITALIC),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                MarkdownElementType.Strikethrough -> paragraph.setSpan(
                                    StrikethroughSpan(),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                else -> {}
                            }
                        }
                    }
                }
            }
            idx++
        }
        flushParagraph()
        return out
    }

    private fun buildSpanFromInline(elements: List<MarkdownElement>): CharSequence {
        val span = SpannableStringBuilder()
        var idx = 0
        while (idx < elements.size) {
            val e = elements[idx]
            val start = span.length
            when (e.type) {
                MarkdownElementType.Image -> {
                    span.append(e.text.ifBlank { "[img]" })
                }

                MarkdownElementType.ListItem -> {
                    span.append("• ").append(e.text).append('\n')
                }

                else -> span.append(e.text)
            }
            val end = span.length
            when (e.type) {
                MarkdownElementType.Bold -> span.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                MarkdownElementType.Italic -> span.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                MarkdownElementType.Strikethrough -> span.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                else -> {}
            }
            idx++
        }
        return span
    }
}
